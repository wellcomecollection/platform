#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Shared library to help surface ECS deployment information.
"""

import collections
import dateutil.parser

from ecs_utils import get_cluster_arns, get_service_arns, describe_service


Deployment = collections.namedtuple('Deployment', 'deployment_key deployment_status color created_at task_definition')
DeploymentKey = collections.namedtuple('Deployment', 'id service_arn')

def _create_deployment_tuple_from_item(item):
    return Deployment(
        DeploymentKey(item['deployment_id'],item['service_arn']),
        item['deployment_status'],
        item['color'],
        dateutil.parser.parse(item['created_at'],""),
        item['task_definition']
    )

def _create_deployment_tuple_from_ecs(service, deployment):
    deployment_status = deployment['status']
    ongoing_deployment = len(service['deployments']) > 1

    color = "blue"
    if(ongoing_deployment):
        color = "green" if(deployment_status == "PRIMARY") else "blue"

    return Deployment(
        DeploymentKey(deployment['id'], service['serviceArn']),
        deployment_status,
        color,
        deployment['createdAt'],
        deployment['taskDefinition']
    )

def _get_service_deployments(ecs_client, cluster_arn, service_arn):
    service = describe_service(ecs_client, cluster_arn, service_arn)

    return [_create_deployment_tuple_from_ecs(service, deployment) for deployment in service['deployments']]

def get_deployments_from_dynamo(table):
    response = table.scan()

    return [_create_deployment_tuple_from_item(d) for d in response['Items']]

def get_deployments_from_ecs(ecs_client):
    deployments=[]

    cluster_arns = get_cluster_arns(ecs_client)
    for cluster_arn in cluster_arns:
        service_arns = get_service_arns(ecs_client, cluster_arn)
        for service_arn in service_arns:
            service_deployments = _get_service_deployments(ecs_client, cluster_arn, service_arn)
            deployments += service_deployments

    return deployments