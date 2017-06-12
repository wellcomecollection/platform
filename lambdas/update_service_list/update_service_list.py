#!/usr/bin/env python
# -*- encoding: utf-8 -*-

"""
Publish a file containing a summary of ECS service status to S3.

This script runs when triggered from an ECS task state change stream.
It does not use the event data from the event.
"""

import boto3

import json

import pprint


client = boto3.client('ecs')


def _name_from_arn(arn):
    return arn.split("/")[1]


def get_cluster_names():
    response = client.list_clusters()
    cluster_names = []
    for cluster_arn in response['clusterArns']:
        cluster_names.apend(_name_from_arn(cluster_arn))

    return cluster_names


def get_service_names(cluster_name):
    response = client.list_services(
        cluster=cluster_name
    )
    service_names = []
    for service_arn in response['serviceArns']:
        service_names.append(_name_from_arn(service_arn))

    return service_names


def describe_services(cluster_name, service_name):
    response = client.describe_services(
        cluster=cluster_name,
        services=[service_name]
    )

    return response['services']


def _create_service_dict(service):
    return {
        'serviceName': service['serviceName'],
        'desiredCount': service['desiredCount'],
        'pendingCount': service['pendingCount'],
        'runningCount': service['runningCount'],
        'status': service['status']
    }


def get_service_list():
    for cluster_name in get_cluster_names():
        service_list = []
        for service_name in get_service_names(cluster_name):
            service = describe_services(cluster_name, service_name)
            service_list.append(create_service_dict(service))

        return service_list


def send_service_list_to_s3():
    client = boto3.client('s3')

    bucket_name = os.environ["BUCKET_NAME"]
    object_key = os.environ["OBJECT_KEY"]

    json_service_list = get_service_list
    print(json_service_list)

    return client.put_object(
        Bucket=bucket_name,
        Key=object_key,
        Body=json.dumps(json_service_list),
        ContentType='application/json'
    )


def main(event, _):
    send_service_list_to_s3()
