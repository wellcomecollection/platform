# -*- encoding: utf-8 -*-
"""
Publish a new Sierra update window to SNS.
"""

import datetime as dt
import os
import time

import boto3

from wellcome_aws_utils.sns_utils import publish_sns_message


def build_window(minutes):
    """Construct the Sierra update window."""
    seconds = minutes * 60

    end = time.time()
    start = end - seconds

    return {
        'start': dt.datetime.fromtimestamp(start).isoformat(),
        'end': dt.datetime.fromtimestamp(end).isoformat(),
    }


def main(event, _):
    print(f'event = {event!r}')

    topic_arn = os.environ['TOPIC_ARN']
    window_length_minutes = int(os.environ['WINDOW_LENGTH_MINUTES'])
    print(
        f'topic_arn={topic_arn}, window_length_minutes={window_length_minutes}'
    )

    message = build_window(minutes=window_length_minutes)

    sns_client = boto3.client('sns')
    publish_sns_message(
        sns_client=sns_client,
        topic_arn=topic_arn,
        message=message
    )
