# -*- encoding: utf-8 -*-

import datetime as dt
import json
import os
import time

import boto3
import mock

from sierra_window_generator import build_window, main

mock_time = mock.Mock()
mock_time.return_value = time.mktime(dt.datetime(2011, 6, 21).timetuple())


@mock.patch('time.time', mock_time)
def test_build_window():
    assert build_window(minutes=15) == {
        'start': '2011-06-20T23:45:00Z',
        'end': '2011-06-21T00:00:00Z',
    }


@mock.patch('time.time', mock_time)
def test_end_to_end(sns_sqs):
    topic_arn, queue_url = sns_sqs

    os.environ['TOPIC_ARN'] = topic_arn
    os.environ['WINDOW_LENGTH_MINUTES'] = '25'

    # This Lambda doesn't read anything from its event or context
    main(None, None)

    sqs_client = boto3.client('sqs')
    messages = sqs_client.receive_message(
        QueueUrl=queue_url,
        MaxNumberOfMessages=1
    )
    message_body = messages['Messages'][0]['Body']
    inner_message = json.loads(message_body)['Message']
    parsed_message = json.loads(json.loads(inner_message)['default'])

    assert parsed_message == {
        'start': '2011-06-20T23:35:00Z',
        'end': '2011-06-21T00:00:00Z',
    }
