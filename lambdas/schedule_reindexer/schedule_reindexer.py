#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Publish a service scheduler to SNS.

This script runs on a fixed schedule to send an SNS notification to start
one of our adapters.
"""

import os

from sns_utils import publish_sns_message


def main(event, _):
    print(f'Received event: {event!r}')
    new_image = event["Records"][0]["dynamodb"]["NewImage"]
    table_name = new_image["TableName"]["S"]
    current_version = new_image["CurrentVersion"]["N"]
    requested_version = new_image["RequestedVersion"]["N"]
    if current_version == requested_version:
        desired_count = 0
    else:
        desired_count = 1
    publish_sns_message(
        topic_arn=os.environ["TOPIC_ARN"],
        cluster=os.environ["CLUSTER_NAME"],
        service=get_service_name(os.environ["REINDEXERS"], table_name),
        desired_count=desired_count
    )
    print(f"desired_count: {desired_count}")


def get_service_name(reindexers, table_name):
    table_service_dict = dict([line.split("=") for line in (reindexers.splitlines())])
    return table_service_dict[table_name]
