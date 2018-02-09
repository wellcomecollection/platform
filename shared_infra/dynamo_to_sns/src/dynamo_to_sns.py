# -*- encoding: utf-8 -*-
"""
Receives DynamoDB events and publishes the event to an SNS topic.
"""

import os

import boto3

from wellcome_aws_utils.dynamo_event import create_dynamo_events
from wellcome_aws_utils.sns_utils import publish_sns_message


def get_sns_messages(trigger_event, stream_view_type):
    """Given a DynamoDB Stream event, generate messages for sending to SNS.

    ``stream_view_type`` must be one of the following values:

    -   FULL_EVENT, which has "event_type", "old_image" and "new_image" keys
    -   NEW_IMAGE_ONLY or OLD_IMAGE_ONLY, which post the corresponding image
        directly into SNS.

    """
    for dynamo_event in create_dynamo_events(trigger_event):
        if stream_view_type == 'FULL_EVENT':
            yield {
                'event_type': dynamo_event.event_type.name,
                'old_image': dynamo_event.old_image(deserialize_values=True),
                'new_image': dynamo_event.new_image(deserialize_values=True),
            }
        elif stream_view_type == 'OLD_IMAGE_ONLY':
            yield dynamo_event.old_image(deserialize_values=True)
        elif stream_view_type == 'NEW_IMAGE_ONLY':
            yield dynamo_event.new_image(deserialize_values=True)
        else:
            raise ValueError(
                f"Unrecognised stream view type: {stream_view_type!r}"
            )


def main(event, _ctxt=None, sns_client=None):
    print(f'Received event: {event!r}')

    topic_arn = os.environ['TOPIC_ARN']
    stream_view_type = os.environ.get('STREAM_VIEW_TYPE', 'FULL_EVENT')

    sns_client = sns_client or boto3.client('sns')

    for message in get_sns_messages(
        trigger_event=event,
        stream_view_type=stream_view_type
    ):
        publish_sns_message(
            sns_client=sns_client,
            topic_arn=topic_arn,
            message=message
        )
