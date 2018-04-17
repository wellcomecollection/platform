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
    es_index = "es_index"

    patched_os_environ = {
        'TOPIC_ARN': topic_arn,
        'PRIVATE_BUCKET_NAME': private_bucket_name,
        'ES_INDEX': es_index
    }

    with patch.dict(os.environ, patched_os_environ, clear=True):
        snapshot_scheduler.main(sns_client=sns_client)

    messages = sns_client.list_messages()
    assert len(messages) == 1
    assert messages[0][':message'] == {
        'time': '2011-06-21T00:00:00',
        'private_bucket_name': private_bucket_name,
        'es_index': es_index
    }
