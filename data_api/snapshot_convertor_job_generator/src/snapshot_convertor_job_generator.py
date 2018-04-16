# -*- encoding: utf-8 -*-
"""

"""

import os

import boto3

from wellcome_aws_utils import s3_utils, sns_utils


def _run(event, sns_client, topic_arn):
    s3_events = s3_utils.parse_s3_record(event=event)

    for s3_event in s3_events:

        event_type = s3_event['event_name']

        if not event_type.startswith('ObjectCreated:'):
            print(f'event_type={event_type!r}, so nothing to do')
            continue

        # This is the job format accepted by the snapshot_convertor
        message = {
            "privateBucketName": s3_event['bucket_name'],
            "sourceObjectKey": s3_event['object_key'],
            "targetBucketName": os.environ['TARGET_BUCKET_NAME'],
            "targetObjectKey": os.environ['TARGET_OBJECT_KEY'],
        }

        print(f'Publishing message {message} to SNS')

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
