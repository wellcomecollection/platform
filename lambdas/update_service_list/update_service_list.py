#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Publish a file containing a summary of ECS service status to S3.

This script runs when triggered from an ECS task state change stream.
It does not use the event data from the event.
"""

import json
import os
import pprint

import boto3

from ecs_utils import get_cluster_arns, get_service_arns, describe_service


def _create_service_dict(service):
    return {
        'serviceName': service['serviceName'],
        'desiredCount': service['desiredCount'],
        'pendingCount': service['pendingCount'],
        'runningCount': service['runningCount'],
        'status': service['status']
    }


def get_service_list(ecs_client):
    service_list = []

    for cluster_arn in get_cluster_arns(ecs_client):
        for service_arn in get_service_arns(ecs_client, cluster_arn):
            service = describe_service(ecs_client, cluster_arn, service_arn)
            service_list.append(_create_service_dict(service))

    return service_list


def send_service_list_to_s3(ecs_client, s3_client, bucket_name, object_key):
    json_service_list = get_service_list(ecs_client)
    pprint.pprint(json_service_list)

    return s3_client.put_object(
        ACL='public-read',
        Bucket=bucket_name,
        Key=object_key,
        Body=json.dumps(json_service_list),
        ContentType='application/json'
    )


def main(event, _):
    pprint.pprint(event)

    ecs_client = boto3.client('ecs')
    s3_client = boto3.client('s3')

    bucket_name = os.environ["BUCKET_NAME"]
    object_key = os.environ["OBJECT_KEY"]

    send_service_list_to_s3(ecs_client, s3_client, bucket_name, object_key)
