# -*- encoding: utf-8 -*-

import datetime as dt
import os

import mock
from unittest.mock import patch

import snapshot_scheduler


class patched_datetime(dt.datetime):
    @classmethod
    def utcnow(cls):
        return dt.datetime(2011, 6, 21, 0, 0, 0, 0)


@mock.patch('datetime.datetime', patched_datetime)
def test_writes_message_to_sqs(sns_client, topic_arn):
    public_bucket_name = 'public-bukkit'
    public_object_key_v1 = 'v1/works.json.gz'
    public_object_key_v2 = 'v2/works.json.gz'

    patched_os_environ = {
        'TOPIC_ARN': topic_arn,
        'PUBLIC_BUCKET_NAME': public_bucket_name,
        'PUBLIC_OBJECT_KEY_V1': public_object_key_v1,
        'PUBLIC_OBJECT_KEY_V2': public_object_key_v2
    }

    with patch.dict(os.environ, patched_os_environ, clear=True):
        snapshot_scheduler.main(sns_client=sns_client)

    messages = sns_client.list_messages()
    assert len(messages) == 2
    assert [m[':message'] for m in messages] == [
        {
            'publicBucketName': public_bucket_name,
            'publicObjectKey': public_object_key_v1,
            'apiVersion': 'v1',
        },
        {
            'publicBucketName': public_bucket_name,
            'publicObjectKey': public_object_key_v2,
            'apiVersion': 'v2',
        },
    ]
