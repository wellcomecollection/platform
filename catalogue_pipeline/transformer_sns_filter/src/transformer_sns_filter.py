# -*- encoding: utf-8 -*-
"""
Receives SNS events of the form

    {
        "event_type": ...,
        "old_image": ...,
        "new_image": ...,
    }

and republishes to a new SNS topic with just the contents of the
``new_image`` key.

"""

import boto3

from wellcome_aws_utils.dynamo_event import create_dynamo_events
from wellcome_aws_utils.sns_utils import (
    extract_sns_messages_from_lambda_event, publish_sns_message
)


def main(event, _):
    print(f'Received event:\n{event}')

    sns_client = boto3.client('sns')

    for sns_event in extract_sns_messages_from_lambda_event(event):
        publish_sns_message(
            topic_arn=topic_arn,
            message=sns_event.message['new_image'],
            sns_client=sns_client
        )
