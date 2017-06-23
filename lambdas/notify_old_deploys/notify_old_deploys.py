#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Checks the DynamoDB deployments table for deployments with a createdAt date
older than the given AGE_BOUNDARY_MINS.

Publishes a message to  given SNS topic if any out of date deployments are
found.

This lambda is intended to be run on a repeated schedule of some period less
than AGE_BOUNDARY_MINS.
"""

import collections
import dateutil.parser
from datetime import datetime, timedelta, timezone
from functools import partial
import os
import pprint

import boto3

from sns_utils import publish_sns_message

Deployment = collections.namedtuple('Deployment', 'deployment_key deployment_status color created_at task_definition')
DeploymentKey = collections.namedtuple('Deployment', 'id service_arn')

def _create_deployment_tuple_from_item(item):
    return Deployment(
        DeploymentKey(item['deployment_id'],item['service_name']),
        item['deployment_status'],
        item['color'],
        dateutil.parser.parse(item['created_at'],""),
        item['task_definition']
    )

def get_deployments_from_dynamo(table):
    response = table.scan()

    return [_create_deployment_tuple_from_item(d) for d in response['Items']]

def _old_deployment(age_boundary_mins, deployment):
    age_boundary = datetime.now(timezone.utc) - timedelta(minutes = 5)

    return (deployment.created_at < age_boundary) and deployment.color == "green"

def filter_old_deployments(deployments, age_boundary_mins):
    filter_func = partial(_old_deployment, age_boundary_mins)
    return list(filter(filter_func, deployments))

def publish_deployments(topic_arn, deployments):
    if(len(old_deployments) > 0):
        publish_sns_message(topic_arn, old_deployments)

def main(event, _):
    print(f'Received event:\n{pprint.pformat(event)}')

    table_name = os.environ["TABLE_NAME"]
    topic_arn = os.environ["TOPIC_ARN"]
    age_boundary_mins = int(os.environ["AGE_BOUNDARY_MINS"])

    dynamodb = boto3.resource('dynamodb')
    table = dynamodb.Table(table_name)

    deployments = get_deployments_from_dynamo(table)
    old_deployments = filter_old_deployments(deployments, age_boundary_mins)
    publish_deployments(topic_arn, old_deployments)

    print(f'Received old deployments:\n{pprint.pformat(old_deployments)}')
