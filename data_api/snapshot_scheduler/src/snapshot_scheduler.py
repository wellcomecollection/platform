# -*- encoding: utf-8 -*-
"""
Publish a new Snapshot Schedule to SNS.
"""

import datetime as dt
import os

import boto3


from wellcome_aws_utils.sns_utils import publish_sns_message


def main(event=None, _ctxt=None, sns_client=None):
    print(f'event = {event!r}')
    sns_client = sns_client or boto3.client('sns')

    topic_arn = os.environ['TOPIC_ARN']
    print(f'topic_arn={topic_arn}')

    message = {
        'time': dt.datetime.utcnow().isoformat()
    }

    publish_sns_message(
        sns_client=sns_client,
        topic_arn=topic_arn,
        message=message,
        subject='source: snapshot_generator.main'
    )
