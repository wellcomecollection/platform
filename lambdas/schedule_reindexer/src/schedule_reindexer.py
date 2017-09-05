#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Publish a service schedule request for the reindexer to SNS.

This script is triggered by updates to the reindexer DynamoDB table.
"""

import os

from dynamo_utils import DynamoEvent
from sns_utils import publish_sns_message


def get_service_name(reindexers, table_name):
    table_service_dict = dict([line.split("=")
                               for line in reindexers.splitlines()])
    return table_service_dict[table_name]


def main(event, _):
    print(f'event = {event!r}')

    dynamo_event = DynamoEvent(event)
    table_name = dynamo_event.new_image["TableName"]["S"]
    current_version = dynamo_event.new_image["CurrentVersion"]["N"]
    requested_version = dynamo_event.new_image["RequestedVersion"]["N"]

    if current_version == requested_version:
        desired_count = 0
        desired_capacity = 1
    else:
        desired_count = 1
        desired_capacity = int(os.environ["DYNAMO_DESIRED_CAPACITY"])

    scheduler_topic_arn = os.environ["SCHEDULER_TOPIC_ARN"]
    cluster = os.environ["CLUSTER_NAME"]
    service = get_service_name(os.environ["REINDEXERS"], table_name)
    message = {'cluster': cluster, 'service': service,
               'desired_count': desired_count}

    print(f'message = {message!r}')
    print(f'scheduler_topic_arn = {scheduler_topic_arn!r}')
    publish_sns_message(topic_arn=scheduler_topic_arn, message=message)

    dynamo_topic_arn = os.environ["DYNAMO_TOPIC_ARN"]
    dynamo_table_name = os.environ["DYNAMO_TABLE_NAME"]
    message = {'dynamo_table_name': dynamo_table_name,
               'desired_capacity': desired_capacity}

    print(f'message = {message!r}')
    publish_sns_message(topic_arn=dynamo_topic_arn, message=message)
