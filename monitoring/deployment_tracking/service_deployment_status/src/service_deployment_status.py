#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Tracking deployment status in ECS is hard!

This lambda tracks deployment status from the ECS Task State change event
stream and writes deployment "color" to dynamo. With color being blue or green.
"""

import os

import boto3

from wellcome_lambda_utils.ecs_utils import (
    EcsThrottleException
)

from wellcome_lambda_utils.deployment_utils import (
    get_deployments_from_ecs,
    get_deployments_from_dynamo,
    put_deployment_in_dynamo,
    delete_deployment_in_dynamo,
    update_deployment_in_dynamo
)


def _find_in_deployments(deployment_list, key):
    return [d for d in deployment_list if d.deployment_key == key][0]


def compare_deployments(current_deployments, last_deployments):
    current_deployments_keys = set(
        [current_deployment.deployment_key
         for current_deployment in current_deployments])

    last_deployments_keys = set([last_deployment.deployment_key
                                 for last_deployment in last_deployments])

    deletions_keys = last_deployments_keys - current_deployments_keys
    additions_keys = current_deployments_keys - last_deployments_keys
    remaining_keys = last_deployments_keys & current_deployments_keys

    unchanged_deployments = set(current_deployments) & set(last_deployments)

    deleted_deployments = [
        _find_in_deployments(last_deployments, deletion_key)
        for deletion_key in deletions_keys
    ]

    added_deployments = [
        _find_in_deployments(current_deployments, additions_key)
        for additions_key in additions_keys
    ]

    maybe_updated_deployments = [
        _find_in_deployments(current_deployments, remaining_key)
        for remaining_key in remaining_keys
    ]

    updated_deployments = list(
        set(maybe_updated_deployments) - unchanged_deployments
    )

    return {
        'deletions': deleted_deployments,
        'additions': added_deployments,
        'updates': updated_deployments
    }


def run_operations(operations, table):
    return {
        "delete_results": [delete_deployment_in_dynamo(table, deployment)
                           for deployment in operations["deletions"]],
        "put_results": [put_deployment_in_dynamo(table, deployment)
                        for deployment in operations["additions"]],
        "update_results": [update_deployment_in_dynamo(table, deployment)
                           for deployment in operations["updates"]]
    }


def main(event, _):
    print(f'event = {event!r}')

    table_name = os.environ["TABLE_NAME"]

    ecs_client = boto3.client('ecs')
    dynamodb = boto3.resource('dynamodb')
    table = dynamodb.Table(table_name)

    try:
        current_deployments = get_deployments_from_ecs(ecs_client)
    except EcsThrottleException:
        # We do not wish to retry on throttle so fail gracefully
        return

    last_deployments = get_deployments_from_dynamo(table)
    operations = compare_deployments(current_deployments, last_deployments)

    ops = run_operations(operations, table)

    print(f'ops = {ops!r}')
