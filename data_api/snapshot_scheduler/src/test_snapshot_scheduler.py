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
    snapshot_scheduler.main(sns_client=sns_client)

    messages = sns_client.list_messages()
    assert len(messages) == 1
    assert messages[0][':message'] == {
        'time': '2011-06-21T00:00:00',
    }
