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
    private_bucket_name = "private_bucket_name"
    public_bucket_name = 'public-bukkit'
    public_object_key_v1 = 'v1/works.json.gz'
    public_object_key_v2 = 'v2/works.json.gz'
    es_index_v1 = "es_index_v1"
    es_index_v2 = "es_index_v2"

    patched_os_environ = {
        'TOPIC_ARN': topic_arn,
        'PRIVATE_BUCKET_NAME': private_bucket_name,
        'PUBLIC_BUCKET_NAME': public_bucket_name,
        'PUBLIC_OBJECT_KEY_V1': public_object_key_v1,
        'PUBLIC_OBJECT_KEY_V2': public_object_key_v2,
        'ES_INDEX_V1': es_index_v1,
        'ES_INDEX_V2': es_index_v2,
    }

    with patch.dict(os.environ, patched_os_environ, clear=True):
        snapshot_scheduler.main(sns_client=sns_client)

    messages = sns_client.list_messages()
    assert len(messages) == 2
    assert [m[':message'] for m in messages] == [
        {
            'time': '2011-06-21T00:00:00',
            'private_bucket_name': private_bucket_name,
            'public_bucket_name': public_bucket_name,
            'public_object_key': public_object_key_v1,
            'es_index': es_index_v1,
            'api_version': 'v1',
        },
        {
            'time': '2011-06-21T00:00:00',
            'private_bucket_name': private_bucket_name,
            'public_bucket_name': public_bucket_name,
            'public_object_key': public_object_key_v2,
            'es_index': es_index_v2,
            'api_version': 'v2',
        },
    ]
