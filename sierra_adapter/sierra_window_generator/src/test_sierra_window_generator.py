# -*- encoding: utf-8 -*-

import datetime as dt
import json
import os

import boto3
import mock

from sierra_window_generator import build_window, main


class patched_datetime(dt.datetime):
    @classmethod
    def utcnow(cls):
        return dt.datetime(2011, 6, 21, 0, 0, 0, 0)


@mock.patch('datetime.datetime', patched_datetime)
def test_build_window():
    assert build_window(minutes=15) == {
        'start': '2011-06-20T23:45:00+00:00',
        'end': '2011-06-21T00:00:00+00:00',
    }


@mock.patch('datetime.datetime', patched_datetime)
def test_end_to_end(sns_client, topic_arn):
    os.environ['WINDOW_LENGTH_MINUTES'] = '25'

    # This Lambda doesn't read anything from its event or context
    main(sns_client=sns_client)

    messages = sns_client.list_messages()
    assert len(messages) == 1
    assert messages[0][':message'] == {
        'start': '2011-06-20T23:35:00+00:00',
        'end': '2011-06-21T00:00:00+00:00',
    }
