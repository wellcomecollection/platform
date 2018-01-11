# -*- encoding: utf-8 -*-

import json
import os

import boto3

from wellcome_aws_utils import s3_utils, sns_utils


def main(event, _):
    print(f'event = {event!r}')

    topic_arn = os.environ["TOPIC_ARN"]

    s3_events = s3_utils.parse_s3_record(event=event)
    assert len(s3_events) == 1
    s3_event = s3_events[0]

    s3_client = boto3.client('s3')
    resp = s3_client.get_object(
        Bucket=s3_event['bucket_name'],
        Key=s3_event['object_key']
    )
    body = resp['Body'].read()

    sns_client = boto3.client('sns')

    records = json.loads(body)
    for r in records:
        sns_utils.publish_sns_message(
            sns_client=sns_client,
            topic_arn=topic_arn,
            message=r
        )
