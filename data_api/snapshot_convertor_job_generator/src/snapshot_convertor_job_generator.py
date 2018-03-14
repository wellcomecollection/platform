# -*- encoding: utf-8 -*-
"""

"""

import json
import os

import boto3

from wellcome_aws_utils import s3_utils, sns_utils


def _run(event, sns_client, topic_arn):
    s3_events = s3_utils.parse_s3_record(event=event)

    for s3_event in s3_events:

        event_type = s3_event['event_name']

        if "s3:ObjectCreated" not in event_type:
            continue

        # This is the job format accepted by the snapshot_convertor
        message = {
            "bucketName": s3_event['bucket_name'],
            "objectKey": s3_event['object_key']
        }

        sns_utils.publish_sns_message(
            sns_client=sns_client,
            topic_arn=topic_arn,
            message=message,
            subject='source: snapshot_convertor_job_generator.main'
        )


def main(event, _ctxt=None, sns_client=None):
    print(f'event = {event!r}')

    topic_arn = os.environ["TOPIC_ARN"]
    sns_client = sns_client or boto3.client('sns')

    _run(event, sns_client, topic_arn)
