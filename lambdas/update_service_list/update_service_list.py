#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Publish a file containing a summary of ECS service status to S3.

This script runs when triggered from an ECS task state change stream.
It does not use the event data from the event.
"""

import datetime
import json
import os
import pprint

import boto3

from ecs_utils import get_cluster_arns, get_service_arns, describe_service, describe_cluster


def _create_event_dict(event):
    return {
        'timestamp': event['createdAt'].timestamp(),
        'message': event['message']
    }

def _create_cluster_dict(cluster, service_list):
    return {
        'clusterName': cluster['clusterName'],
        'status': cluster['status'],
        'instanceCount': cluster['registeredContainerInstancesCount'],
        'serviceList': service_list
    }

def _create_service_dict(service):
    deployments = [ _create_deployment_dict(d) for d in service['deployments'] ]
    # Grab just the last few events to keep the file size down
    events = [ _create_event_dict(e) for e in service['events'][:5] ]

    return {
        'serviceName': service['serviceName'],
        'desiredCount': service['desiredCount'],
        'pendingCount': service['pendingCount'],
        'runningCount': service['runningCount'],
        'deployments': deployments,
        'events' : events,
        'status': service['status']
    }

def _create_deployment_dict(deployment):
    return {
        'id': deployment['id'],
        'taskDefinition': deployment['taskDefinition'],
        'status': deployment['status']
    }

def get_service_list(ecs_client, cluster_arn):
    service_list = []

    for service_arn in get_service_arns(ecs_client, cluster_arn):
        service = describe_service(ecs_client, cluster_arn, service_arn)
        service_list.append(_create_service_dict(service))

    return service_list


def get_cluster_list(ecs_client):
    cluster_list = []

    for cluster_arn in get_cluster_arns(ecs_client):
        cluster = describe_cluster(ecs_client, cluster_arn)
        service_list = get_service_list(ecs_client, cluster_arn)
        cluster_list.append(_create_cluster_dict(cluster, service_list))

    return cluster_list

def send_ecs_status_to_s3(ecs_client, s3_client, bucket_name, object_key):
    cluster_list = get_cluster_list(ecs_client)
    service_snapshot = {
        'clusterList': cluster_list,
        'lastUpdated': str(datetime.datetime.utcnow())
    }

    pprint.pprint(service_snapshot)

    return s3_client.put_object(
        ACL='public-read',
        Bucket=bucket_name,
        Key=object_key,
        Body=json.dumps(service_snapshot),
        ContentType='application/json'
    )


def main(event, _):
    pprint.pprint(event)

    ecs_client = boto3.client('ecs')
    s3_client = boto3.client('s3')

    bucket_name = os.environ["BUCKET_NAME"]
    object_key = os.environ["OBJECT_KEY"]

    send_ecs_status_to_s3(ecs_client, s3_client, bucket_name, object_key)
