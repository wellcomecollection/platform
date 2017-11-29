# -*- encoding: utf-8 -*-
"""
Receives DynamoDB events and publishes the new image from
the event to an SNS topic
"""

import json
import os

import boto3

from wellcome_aws_utils.dyanmo_event import create_dynamo_events
from wellcome_aws_utils.sns_utils import publish_sns_message


def main(event, _):
    print(f'Received event:\n{event}')

    client = boto3.client('sns')
    topic_arn = os.environ['TOPIC_ARN']

    for dynamo_event in create_dynamo_events(event):
        print(dynamo_event)
        parsed_event = {
            'event_type': dynamo_event.event_type.name,
            'old_image': dynamo_event.old_image(deserialize_values=True),
            'new_image': dynamo_event.new_image(deserialize_values=True),
        }
        json_message = json.dumps(parsed_event, separators=(',', ':'))
        publish_sns_message(
            topic_arn=topic_arn,
            message=json_message,
            client=client
        )
