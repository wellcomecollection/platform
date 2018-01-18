#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Publish a service schedule request for the reindexer to SNS.

This script is triggered by updates to the reindexer DynamoDB table.
"""

import os

import boto3

from wellcome_lambda_utils.dynamo_utils import DynamoImageFactory
from wellcome_lambda_utils.sns_utils import publish_sns_message


def get_service_name(reindexers, table_name):
    table_service_dict = dict(
        [line.split("=") for line in reindexers.splitlines()])
    return table_service_dict[table_name]


def _determine_desired_count(record):
    current_version = record.new_image["CurrentVersion"]["N"]
    requested_version = record.new_image["RequestedVersion"]["N"]

    if current_version == requested_version:
        return 0
    else:
        return 1


def _request_task_run(
        sns_client,
        scheduler_topic_arn,
        cluster_name,
        service,
        desired_count):
    message = {
        'cluster': cluster_name,
        'service': service,
        'desired_count': desired_count
    }

    print(f'message = {message!r}')
    print(f'scheduler_topic_arn = {scheduler_topic_arn!r}')

    publish_sns_message(
        sns_client=sns_client,
        topic_arn=scheduler_topic_arn,
        message=message
    )


def main(event, _):
    print(f'event = {event!r}')

    scheduler_topic_arn = os.environ['SCHEDULER_TOPIC_ARN']
    cluster_name = os.environ['CLUSTER_NAME']
    reindexers = os.environ['REINDEXERS']

    sns_client = boto3.client('sns')

    for record in DynamoImageFactory.create(event):
        table_name = record.new_image["TableName"]["S"]
        service = get_service_name(
            reindexers=reindexers,
            table_name=table_name
        )

        desired_count = _determine_desired_count(record=record)

        _request_task_run(
            sns_client=sns_client,
            scheduler_topic_arn=scheduler_topic_arn,
            cluster_name=cluster_name,
            service=service,
            desired_count=desired_count
        )

        print("Done.")
