#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Receives DynamoDB events and publishes the event to an SNS topic

"""

import os

import boto3

from wellcome_lambda_utils.dynamo_utils import DynamoEventFactory, DynamoEventType
from wellcome_lambda_utils.sns_utils import publish_sns_message


def main(event, _):
    print(f'Received event:\n{event}')

    sns_client = boto3.client('sns')

    remove_topic_arn = os.environ["REMOVE_TOPIC_ARN"]
    modify_topic_arn = os.environ["MODIFY_TOPIC_ARN"]
    insert_topic_arn = os.environ["INSERT_TOPIC_ARN"]

    for dynamo_event in DynamoEventFactory.create(event):

        if dynamo_event.event_type == DynamoEventType.REMOVE:
            topic_arn = remove_topic_arn
            subject = "REMOVE"
        if dynamo_event.event_type == DynamoEventType.MODIFY:
            topic_arn = modify_topic_arn
            subject = "MODIFY"
        if dynamo_event.event_type == DynamoEventType.INSERT:
            topic_arn = insert_topic_arn
            subject = "INSERT"

        if (not topic_arn) or (not subject):
            raise Exception(f'Topic or Subject unset based on {dynamo_event}')

        publish_sns_message(
            topic_arn=topic_arn,
            message=dynamo_event,
            subject=subject,
            sns_client=sns_client
        )
