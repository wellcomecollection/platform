#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Tracking deployment status in ECS is hard!

This lambda tracks deployment status from the ECS Task State change event
stream and writes deployment "color" to dynamo. With color being blue or green.
"""

import collections
from datetime import datetime
import dateutil.parser
import os
import pprint

import boto3

from ecs_utils import get_cluster_arns, get_service_arns, describe_service, describe_cluster


Deployment = collections.namedtuple('Deployment', 'deployment_key deployment_status color created_at task_definition')
DeploymentKey = collections.namedtuple('Deployment', 'id service_arn')

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

def _create_deployment_tuple_from_item(item):
    return Deployment(
        DeploymentKey(item['deployment_id'],item['service_arn']),
        item['deployment_status'],
        item['color'],
        dateutil.parser.parse(item['created_at'],""),
        item['task_definition']
    )


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

def find_in_deployments(deployment_list, key):
    return [deployment for deployment in deployment_list if deployment[0] == key][0]

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
            'created_at': str(deployment.created_at),
            'task_definition': deployment.task_definition
        }
    )

def update_deployment_in_dynamo(table, deployment):
    return table.update_item(
        Key={
            'deployment_id': deployment.deployment_key.id,
            'service_name': deployment.deployment_key.service_arn
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
            ':created_at': str(deployment.created_at),
            ':task_definition': deployment.task_definition
        }
    )

def compare_deployments(current_deployments, last_deployments):
    current_deployments_keys = set([current_deployment[0] for current_deployment in current_deployments])
    last_deployments_keys = set([last_deployment[0] for last_deployment in last_deployments])

    deletions_keys = last_deployments_keys - current_deployments_keys
    additions_keys = current_deployments_keys - last_deployments_keys
    remaining_keys = last_deployments_keys & current_deployments_keys

    unchanged_deployments = set(current_deployments) & set(last_deployments)

    deleted_deployments = [find_in_deployments(last_deployments, deletion_key) for deletion_key in deletions_keys]
    added_deployments = [find_in_deployments(current_deployments, additions_key) for additions_key in additions_keys]
    maybe_updated_deployments = [find_in_deployments(current_deployments, remaining_key) for remaining_key in remaining_keys]

    updated_deployments = list(set(maybe_updated_deployments) - unchanged_deployments)

    return {
        'deletions': deleted_deployments,
        'additions': added_deployments,
        'updates': updated_deployments
    }

def run_operations(operations, table):
    return {
        "delete_results": [delete_deployment_in_dynamo(table, deployment) for deployment in operations["deletions"]],
        "put_results": [put_deployment_in_dynamo(table, deployment) for deployment in operations["additions"]],
        "update_results":[update_deployment_in_dynamo(table, deployment) for deployment in operations["updates"]]
    }

def main(event, _):
    print(f'Received event:\n{pprint.pformat(event)}')

    table_name = os.environ["TABLE_NAME"]

    ecs_client = boto3.client('ecs')
    dynamodb = boto3.resource('dynamodb')
    table = dynamodb.Table(table_name)

    current_deployments = get_deployments_from_ecs(ecs_client)
    last_deployments = get_deployments_from_dynamo(table)
    operations = compare_deployments(current_deployments, last_deployments)

    ops = run_operations(operations, table)

    print(f'Run dynamo ops:\n{pprint.pformat(ops)}')
