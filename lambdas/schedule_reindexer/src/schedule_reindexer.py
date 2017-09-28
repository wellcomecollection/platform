#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Publish a service schedule request for the reindexer to SNS.

This script is triggered by updates to the reindexer DynamoDB table.
"""

import os

from utils.dynamo_utils import DynamoImageFactory
from utils.sns_utils import publish_sns_message


def get_service_name(reindexers, table_name):
    table_service_dict = dict([line.split("=")
                               for line in reindexers.splitlines()])
    return table_service_dict[table_name]


def _determine_count_and_capacity(record, desired_capacity):
    current_version = record.new_image["CurrentVersion"]["N"]
    requested_version = record.new_image["RequestedVersion"]["N"]

    if current_version == requested_version:
        desired_count = 0
        desired_capacity = 1
    else:
        desired_count = 1

    return {
        "count": desired_count,
        "capacity": desired_capacity
    }


def _request_task_run(scheduler_topic_arn, cluster_name, service, desired_count):
    message = {
        'cluster': cluster_name,
        'service': service,
        'desired_count': desired_count
    }

    print(f'message = {message!r}')
    print(f'scheduler_topic_arn = {scheduler_topic_arn!r}')

    publish_sns_message(topic_arn=scheduler_topic_arn, message=message)


def _request_dynamo_provision(dynamo_topic_arn, dynamo_table_name, desired_capacity):
    message = {
        'dynamo_table_name': dynamo_table_name,
        'desired_capacity': desired_capacity
    }

    print(f'message = {message!r}')
    print(f'dynamo_topic_arn = {dynamo_topic_arn!r}')

    publish_sns_message(topic_arn=dynamo_topic_arn, message=message)


def main(event, _):
    print(f'event = {event!r}')

    desired_capacity = int(os.environ["DYNAMO_DESIRED_CAPACITY"])

    for record in DynamoImageFactory.create(event):
        table_name = record.new_image["TableName"]["S"]

        desired = _determine_count_and_capacity(record, desired_capacity)

        print(f"Count and capacity required: {desired}")

        _request_task_run(
            os.environ["SCHEDULER_TOPIC_ARN"],
            os.environ["CLUSTER_NAME"],
            get_service_name(os.environ["REINDEXERS"], table_name),
            desired["count"]
        )

        _request_dynamo_provision(
            os.environ["DYNAMO_TOPIC_ARN"],
            os.environ["DYNAMO_TABLE_NAME"],
            desired["capacity"]
        )

        print("Done.")
