# -*- encoding: utf-8 -*-
"""
Publish a new Sierra update window to SNS.
"""

import datetime as dt
import os

import boto3
from wellcome_aws_utils.lambda_utils import log_on_error
from wellcome_aws_utils.sns_utils import publish_sns_message

from build_windows import generate_windows


def build_window(minutes):
    """Construct the Sierra update window."""
    seconds = minutes * 60

    end = dt.datetime.now()
    start = end - dt.timedelta(seconds=seconds)

    return next(generate_windows(start=start, end=end, minutes=minutes))


@log_on_error
def main(event=None, _ctxt=None, sns_client=None):
    sns_client = sns_client or boto3.client("sns")

    topic_arn = os.environ["TOPIC_ARN"]
    window_length_minutes = int(os.environ["WINDOW_LENGTH_MINUTES"])
    print(f"topic_arn={topic_arn}, window_length_minutes={window_length_minutes}")

    message = build_window(minutes=window_length_minutes)

    publish_sns_message(
        sns_client=sns_client,
        topic_arn=topic_arn,
        message=message,
        subject="source: sierra_window_generator.main",
    )
