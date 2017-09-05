#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Change the size of an ECS service.

This is used to schedule our service applications: by setting the desired
size to 0/greater-than-0, Amazon will do the work of spinning up or scaling
down the tasks.

The script is triggered by notifications to an SNS topic, in which the
message should be a JSON string that includes "cluster", "service" and
"desired_count" as attributes.
"""

import json

import boto3


def change_desired_count(cluster, service, desired_count):
    """
    Given an ECS cluster, service name and desired instance count, change
    the instance count on AWS.
    """
    ecs = boto3.client('ecs')
    resp = ecs.update_service(
        cluster=cluster,
        service=service,
        desiredCount=desired_count
    )
    print(f'resp = {resp!r}')
    assert resp['ResponseMetadata']['HTTPStatusCode'] == 200


def main(event, _):
    print(f'event = {event!r}')
    message = event['Records'][0]['Sns']['Message']
    message_data = json.loads(message)

    change_desired_count(
        cluster=message_data['cluster'],
        service=message_data['service'],
        desired_count=message_data['desired_count']
    )
