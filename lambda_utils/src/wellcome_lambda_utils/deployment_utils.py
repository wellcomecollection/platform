#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Shared library to help surface ECS deployment information.
"""

import collections
import datetime

from wellcome_lambda_utils.ecs_utils import get_cluster_arns, get_service_arns, describe_service

Deployment = collections.namedtuple(
    'Deployment',
    'deployment_key deployment_status color created_at task_definition'
)
DeploymentKey = collections.namedtuple('DeploymentKey', 'id service_arn')


DATE_FORMAT = "%Y-%m-%dT%H:%M:%S.%fZ"


def _create_deployment_tuple_from_item(item):
    item_date = datetime.datetime.strptime(item['created_at'], DATE_FORMAT)

    return Deployment(
        DeploymentKey(item['deployment_id'], item['service_arn']),
        item['deployment_status'],
        item['color'],
        item_date,
        item['task_definition']
    )


def _create_deployment_tuple_from_ecs(service, deployment):
    """Takes AWS ECS API Service & Deployment, return Deployment namedtuple."""
    deployment_status = deployment['status']
    ongoing_deployment = len(service['deployments']) > 1

    if ongoing_deployment and (deployment_status == "PRIMARY"):
        color = "green"
    else:
        color = "blue"

    return Deployment(
        DeploymentKey(deployment['id'], service['serviceArn']),
        deployment_status,
        color,
        deployment['createdAt'],
        deployment['taskDefinition']
    )


def _get_service_deployments(ecs_client, cluster_arn, service_arn):
    service = describe_service(ecs_client, cluster_arn, service_arn)

    return [_create_deployment_tuple_from_ecs(service, deployment)
            for deployment in service['deployments']]


def _get_date_string(date):
    return date.strftime(DATE_FORMAT)


def delete_deployment_in_dynamo(table, deployment):
    return table.delete_item(
        Key={
            'deployment_id': deployment.deployment_key.id,
            'service_arn': deployment.deployment_key.service_arn
        }
    )


def put_deployment_in_dynamo(table, deployment):
    return table.put_item(
        Item={
            'deployment_id': deployment.deployment_key.id,
            'service_arn': deployment.deployment_key.service_arn,
            'deployment_status': deployment.deployment_status,
            'color': deployment.color,
            'created_at': _get_date_string(deployment.created_at),
            'task_definition': deployment.task_definition
        }
    )


def update_deployment_in_dynamo(table, deployment):
    return table.update_item(
        Key={
            'deployment_id': deployment.deployment_key.id,
            'service_arn': deployment.deployment_key.service_arn
        },
        UpdateExpression="""
            SET deployment_status = :deployment_status,
                color = :color,
                created_at = :created_at,
                task_definition = :task_definition
        """,
        ExpressionAttributeValues={
            ':deployment_status': deployment.deployment_status,
            ':color': deployment.color,
            ':created_at': _get_date_string(deployment.created_at),
            ':task_definition': deployment.task_definition
        }
    )


def get_deployments_from_dynamo(table):
    response = table.scan()

    return [_create_deployment_tuple_from_item(d) for d in response['Items']]


def get_deployments_from_ecs(ecs_client):
    deployments = []

    for cluster_arn in get_cluster_arns(ecs_client):
        for service_arn in get_service_arns(ecs_client, cluster_arn):
            service_deployments = _get_service_deployments(
                ecs_client, cluster_arn, service_arn)
            deployments += service_deployments

    return deployments
