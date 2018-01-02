# -*- encoding: utf-8 -*-
"""
Receives DynamoDB events and publishes the event to an SNS topic.
"""

import os

import boto3

from wellcome_aws_utils.dynamo_event import create_dynamo_events
from wellcome_aws_utils.sns_utils import publish_sns_message


def get_sns_messages(dynamo_event):
    """Given a DynamoDB Stream event, generate messages for sending to SNS."""
    for dynamo_event in create_dynamo_events(event):
        yield {
            'event_type': dynamo_event.event_type.name,
            'old_image': dynamo_event.old_image(deserialize_values=True),
            'new_image': dynamo_event.new_image(deserialize_values=True),
        }


def main(event, _):
    print(f'Received event:\n{event}')

    topic_arn = os.environ['TOPIC_ARN']

    sns_client = boto3.client('sns')

    for message in get_sns_messages(dynamo_event=event):
        publish_sns_message(
            sns_client=sns_client,
            topic_arn=topic_arn,
            message=message
        )
