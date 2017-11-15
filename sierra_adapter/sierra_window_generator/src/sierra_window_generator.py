# -*- encoding: utf-8 -*-
"""
Publish a new Sierra update window to SNS.
"""

import os

import boto3
import maya

from wellcome_aws_utils.sns_utils import publish_sns_message


def main(event, _):
    print(f'event = {event!r}')

    topic_arn = os.environ['TOPIC_ARN']
    window_start = os.environ['WINDOW_START']

    start = maya.when(window_start)
    now = maya.now()

    message = {
        'start': start.iso8601(),
        'end': now.iso8601(),
    }

    client = boto3.client('sns')
    publish_sns_message(client=client, topic_arn=topic_arn, message=message)
