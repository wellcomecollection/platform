#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Checks the DynamoDB deployments table for deployments with a createdAt date
older than the given AGE_BOUNDARY_MINS.

Publishes a message to a given SNS topic if any out of date deployments are
found.

This lambda is intended to be run on a repeated schedule of some period less
than AGE_BOUNDARY_MINS.
"""

from datetime import datetime, timedelta, timezone
from functools import partial
import os
import pprint

import boto3

from deployment_utils import get_deployments_from_dynamo
from sns_utils import publish_sns_message


def _old_deployment(age_boundary_mins, deployment):
    age_boundary = datetime.now(timezone.utc) - timedelta(minutes = age_boundary_mins)

    return (deployment.created_at < age_boundary) and deployment.color == "green"

def filter_old_deployments(deployments, age_boundary_mins):
    filter_func = partial(_old_deployment, age_boundary_mins)
    return list(filter(filter_func, deployments))

def publish_deployments(topic_arn, deployments):
    if(len(deployments) > 0):
        publish_sns_message(topic_arn, deployments)

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
