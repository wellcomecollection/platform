# -*- encoding: utf-8 -*-
"""
Publish a new Snapshot Schedule to SNS.
"""

import os

import attr
import boto3

from wellcome_aws_utils.sns_utils import publish_sns_message


# This class is duplicated in the elasticdump app
# Changes here will need to be reflected there.
@attr.s
class SnapshotRequest(object):
    apiVersion = attr.ib()
    publicBucketName = attr.ib()
    publicObjectKey = attr.ib()


def main(event=None, _ctxt=None, sns_client=None):
    print(f'event = {event!r}')
    print(os.environ)
    sns_client = sns_client or boto3.client('sns')

    topic_arn = os.environ['TOPIC_ARN']

    public_bucket_name = os.environ['PUBLIC_BUCKET_NAME']

    public_object_key_v1 = os.environ['PUBLIC_OBJECT_KEY_V1']
    public_object_key_v2 = os.environ['PUBLIC_OBJECT_KEY_V2']

    for (api_version, public_object_key) in [
        ('v1', public_object_key_v1),
        ('v2', public_object_key_v2),
    ]:
        snapshot_request_message = SnapshotRequest(
            apiVersion=api_version,
            publicBucketName=public_bucket_name,
            publicObjectKey=public_object_key,
        )

        publish_sns_message(
            sns_client=sns_client,
            topic_arn=topic_arn,
            message=attr.asdict(snapshot_request_message),
            subject='source: snapshot_scheduler.main'
        )
