#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Tracking deployment status in ECS is hard!

This lambda tracks deployment status from the ECS Task State change event
stream and writes deployment "color" to dynamo. With color being blue or green.
"""

import os
import pprint

import boto3

from deployment_utils import get_deployments_from_dynamo, get_deployments_from_ecs


def _find_in_deployments(deployment_list, key):
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

    deleted_deployments = [_find_in_deployments(last_deployments, deletion_key) for deletion_key in deletions_keys]
    added_deployments = [_find_in_deployments(current_deployments, additions_key) for additions_key in additions_keys]
    maybe_updated_deployments = [_find_in_deployments(current_deployments, remaining_key) for remaining_key in remaining_keys]

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
