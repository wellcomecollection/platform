# -*- encoding: utf-8 -*-
"""
Publish a new Snapshot Schedule to SNS.
"""

import datetime as dt
import os

import attr
import boto3

from wellcome_aws_utils.sns_utils import publish_sns_message


# This class is duplicated in the elasticdump app
# Changes here will need to be reflected there.
@attr.s
class SnapshotRequest(object):
    time = attr.ib()
    api_version = attr.ib()
    private_bucket_name = attr.ib()
    public_bucket_name = attr.ib()
    public_object_key = attr.ib()
    es_index = attr.ib()


def main(event=None, _ctxt=None, sns_client=None):
    print(f'event = {event!r}')
    print(os.environ)
    sns_client = sns_client or boto3.client('sns')

    topic_arn = os.environ['TOPIC_ARN']

    private_bucket_name = os.environ['PRIVATE_BUCKET_NAME']
    public_bucket_name = os.environ['PUBLIC_BUCKET_NAME']

    es_index_v1 = os.environ['ES_INDEX_V1']
    es_index_v2 = os.environ['ES_INDEX_V2']

    public_object_key_v1 = os.environ['PUBLIC_OBJECT_KEY_V1']
    public_object_key_v2 = os.environ['PUBLIC_OBJECT_KEY_V2']

    for (api_version, es_index, public_object_key) in [
        ('v1', es_index_v1, public_object_key_v1),
        ('v2', es_index_v2, public_object_key_v2),
    ]:
        snapshot_request_message = SnapshotRequest(
            time=dt.datetime.utcnow().isoformat(),
            api_version=api_version,
            private_bucket_name=private_bucket_name,
            public_bucket_name=public_bucket_name,
            public_object_key=public_object_key,
            es_index=es_index
        )

        publish_sns_message(
            sns_client=sns_client,
            topic_arn=topic_arn,
            message=attr.asdict(snapshot_request_message),
            subject='source: snapshot_scheduler.main'
        )
