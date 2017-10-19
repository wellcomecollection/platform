# -*- encoding: utf-8 -*-
"""
Publish a file containing a summary of ECS service status to S3.

This script runs when triggered from an ECS task state change stream.
It does not use the event data from the event.
"""

import datetime
import json
import os

import boto3

from utils.ecs_utils import (
    get_cluster_arns,
    get_service_arns,
    describe_service,
    describe_cluster,
    EcsThrottleException
)


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
    deployments = [_create_deployment_dict(d) for d in service['deployments']]
    # Grab just the last few events to keep the file size down
    events = [_create_event_dict(e) for e in service['events'][:5]]

    return {
        'serviceName': service['serviceName'],
        'desiredCount': service['desiredCount'],
        'pendingCount': service['pendingCount'],
        'runningCount': service['runningCount'],
        'deployments': deployments,
        'events': events,
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


def send_ecs_status_to_s3(
        service_snapshot,
        s3_client,
        bucket_name,
        object_key):

    return s3_client.put_object(
        ACL='public-read',
        Bucket=bucket_name,
        Key=object_key,
        Body=json.dumps(service_snapshot),
        CacheControl='max-age=0',
        ContentType='application/json'
    )


def create_boto_client(service, role_arn):
    sts_client = boto3.client('sts')

    assumed_role_object = sts_client.assume_role(
        RoleArn=role_arn,
        RoleSessionName="AssumeRoleSession"
    )

    credentials = assumed_role_object['Credentials']

    return boto3.client(
        service_name='ecs',
        aws_access_key_id=credentials['AccessKeyId'],
        aws_secret_access_key=credentials['SecretAccessKey'],
        aws_session_token=credentials['SessionToken']
    )


def main(event, _):
    print(f'event = {event!r}')
    assumable_roles = (
        [s for s in os.environ["ASSUMABLE_ROLES"].split(",") if s]
    )
    bucket_name = os.environ["BUCKET_NAME"]
    object_key = os.environ["OBJECT_KEY"]

    ecs_clients = (
        [create_boto_client('ecs', role_arn) for role_arn in assumable_roles]
    ) + [boto3.client('ecs')]

    try:
        cluster_lists = [
            get_cluster_list(ecs_client) for ecs_client in ecs_clients
        ]
    except EcsThrottleException:
        # We do not wish to retry on throttle so fail gracefully
        return

    cluster_list = [item for sublist in cluster_lists for item in sublist]

    service_snapshot = {
        'clusterList': cluster_list,
        'lastUpdated': str(datetime.datetime.utcnow())
    }

    print(f'service_snapshot = {service_snapshot!r}')

    s3_client = boto3.client('s3')

    response = send_ecs_status_to_s3(
        service_snapshot,
        s3_client,
        bucket_name,
        object_key
    )

    print(f'response = {response}')
