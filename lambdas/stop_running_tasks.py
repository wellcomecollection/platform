#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Stop all the instances of a running task.

Our applications load config from an S3 bucket at startup.  When the S3 config
changes, this Lambda is triggered.  We need to stop any running instances
of the application, then the ECS scheduler will restart them and they'll
pick up their new config.

"""

import re

import boto3


def identify_cluster_by_app_name(ecs, app_name):
    """
    Given the name of one of our applications (e.g. api, calm_adapter),
    return the ARN of the cluster the task runs on.
    """
    for cluster in ecs.list_clusters()['clusterArns']:
        for serviceArn in ecs.list_services(cluster=cluster)['serviceArns']:

            # The format of an ECS service ARN is:
            #
            #     arn:aws:ecs:{aws_region}:{account_id}:service/{service_name}
            #
            # Our ECS cluster is configured so that the name of the ECS cluster
            # matches the name of the config in S3.
            _, serviceName = serviceArn.split('/')
            if serviceName == app_name:
                return cluster

    raise RuntimeError('Unable to find ECS cluster for %s' % app_name)


def stop_running_tasks(app_name):
    """
    Given the name of one of our applications (e.g. api, calm_adapter),
    stop all the running instances of this application.
    """
    ecs = boto3.client('ecs')
    cluster = identify_cluster_by_app_name(ecs=ecs, app_name=app_name)
    taskArns = ecs.list_tasks(
        cluster=cluster,
        serviceName=app_name
    )['taskArns']
    for task in taskArns:
        ecs.stop_task(
            cluster=cluster,
            task=task,
            reason='Restarting to pick up new configuration'
        )


def main(event, _):
    print('Received event: %r' % event)
    records = event['Records']
    assert len(records) == 1
    changed_object_key = records[0]['s3']['object']['key']
    match = re.match(r'^config/prod/(?P<app>[a-z_]+)\.ini', changed_object_key)
    assert match is not None
    app_name = match.group('app')
    print('Stopping tasks for %s' % app_name)
    stop_running_tasks(app_name=app_name)
