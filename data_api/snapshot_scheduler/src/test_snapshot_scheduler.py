# -*- encoding: utf-8 -*-

import datetime as dt

import mock

import snapshot_scheduler


class patched_datetime(dt.datetime):
    @classmethod
    def utcnow(cls):
        return dt.datetime(2011, 6, 21, 0, 0, 0, 0)


@mock.patch('datetime.datetime', patched_datetime)
def test_writes_message_to_sqs(sns_client, topic_arn):
    target_bucket_name = "target_bucket_name"
    es_index = "es_index"

    snapshot_scheduler._run(
        sns_client=sns_client,
        topic_arn=topic_arn,
        target_bucket_name=target_bucket_name,
        es_index=es_index
    )

    messages = sns_client.list_messages()
    assert len(messages) == 1
    assert messages[0][':message'] == {
        'time': '2011-06-21T00:00:00',
        'target_bucket_name': target_bucket_name,
        'es_index': es_index
    }
