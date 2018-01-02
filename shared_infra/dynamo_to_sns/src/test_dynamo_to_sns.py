# -*- encoding: utf-8 -*-

import json
import os

import boto3
import pytest

import dynamo_to_sns


TEST_STREAM_ARN = 'arn:aws:dynamodb:eu-west-1:123456789012:table/table-stream'

OLD_IMAGE = {
    'MiroID': {'S': 'V0000001'},
    'MiroCollection': {'S': 'Images-V'},
}

OLD_IMAGE_DATA = {
    'MiroID': 'V0000001',
    'MiroCollection': 'Images-V'
}

NEW_IMAGE = {
    'ReindexVersion': {'N': '0'},
    'ReindexShard': {'S': 'default'},
    'data': {'S': 'test-json-data'},
    'MiroID': {'S': 'V0010033'},
    'MiroCollection': {'S': 'Images-V'}
}

NEW_IMAGE_DATA = {
    'ReindexVersion': 0,
    'ReindexShard': 'default',
    'data': 'test-json-data',
    'MiroID': 'V0010033',
    'MiroCollection': 'Images-V'
}


def _dynamo_event(event_name, old_image=None, new_image=None):
    event_data = {
        'eventID': '87cf2ca0f689908d573fb3698a487bb1',
        'eventName': event_name,
        'eventVersion': '1.1',
        'eventSource': 'aws:dynamodb',
        'awsRegion': 'eu-west-1',
        'dynamodb': {
            'ApproximateCreationDateTime': 1505815200.0,
            'Keys': {
                'MiroID': {
                    'S': 'V0000001'
                },
                'MiroCollection': {
                    'S': 'Images-V'
                }
            },
            'OldImage': old_image,
            'SequenceNumber': '545308300000000005226392296',
            'SizeBytes': 36,
            'StreamViewType': 'OLD_IMAGE'
        },
        'eventSourceARN': TEST_STREAM_ARN
    }

    if old_image is not None:
        event_data['dynamodb']['OldImage'] = old_image
    if new_image is not None:
        event_data['dynamodb']['NewImage'] = new_image

    return event_data


@pytest.mark.parametrize('input_event, expected_message', [
    (
        _dynamo_event(event_name='MODIFY', new_image=NEW_IMAGE),
        {
            'event_type': 'MODIFY',
            'old_image': None,
            'new_image': NEW_IMAGE_DATA
        }
    ),
    (
        _dynamo_event(event_name='REMOVE', old_image=OLD_IMAGE),
        {
            'event_type': 'REMOVE',
            'old_image': OLD_IMAGE_DATA,
            'new_image': None
        },
    ),
])
def test_end_to_end_feature_test(queue_url, input_event, expected_message):
    event = {'Records': [input_event]}
    dynamo_to_sns.main(event=event, context=None)

    _assert_sqs_has_message(
        expected_message=expected_message,
        queue_url=queue_url
    )


def _assert_sqs_has_message(expected_message, queue_url):
    sqs_client = boto3.client('sqs')
    messages = sqs_client.receive_message(
        QueueUrl=queue_url,
        MaxNumberOfMessages=1
    )

    assert len(messages['Messages']) == 1

    body = messages['Messages'][0]['Body']
    inner_msg = json.loads(body)['Message']
    assert json.loads(json.loads(inner_msg)['default']) == expected_message
