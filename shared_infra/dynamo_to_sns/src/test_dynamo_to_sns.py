# -*- encoding: utf-8 -*-

import json
import os

import boto3

import dynamo_to_sns


TEST_STREAM_ARN = 'arn:aws:dynamodb:eu-west-1:123456789012:table/table-stream'


def test_dynamo_to_sns_fails_gracefully_on_remove_event(sns_sqs):
    sqs_client = boto3.client('sqs')
    topic_arn, queue_url = sns_sqs

    os.environ = {
        'TOPIC_ARN': topic_arn
    }

    old_image = {
        'MiroID': {'S': 'V0000001'},
        'MiroCollection': {'S': 'Images-V'},
    }

    expected_image = {
        "event_type": "REMOVE",
        "new_image": None,
        "old_image": {
            "MiroID": "V0000001",
            "MiroCollection": "Images-V"
        }
    }

    remove_event = {
        'Records': [
            _dynamo_event(event_name='REMOVE', old_image=old_image)
        ]
    }

    dynamo_to_sns.main(remove_event, None)

    _assert_sqs_has_messages(
        expected_messages=[expected_image],
        queue_url=queue_url
    )


def test_dynamo_to_sns(sns_sqs):
    sqs_client = boto3.client('sqs')
    topic_arn, queue_url = sns_sqs
    stream_arn = 'arn:aws:dynamodb:eu-west-1:123456789012:table/table-stream'

    new_image = {
        'ReindexVersion': {'N': '0'},
        'ReindexShard': {'S': 'default'},
        'data': {'S': 'test-json-data'},
        'MiroID': {'S': 'V0010033'},
        'MiroCollection': {'S': 'Images-V'}
    }

    expected_image = {
        "event_type": "MODIFY",
        "old_image": None,
        "new_image": {
            "ReindexVersion": 0,
            "ReindexShard": "default",
            "data": "test-json-data",
            "MiroID": "V0010033",
            "MiroCollection": "Images-V"
        }
    }

    event = {
        'Records': [
            _dynamo_event(event_name='MODIFY', new_image=new_image)
        ]
    }

    os.environ = {
        'TOPIC_ARN': topic_arn
    }

    dynamo_to_sns.main(event, None)

    _assert_sqs_has_messages(
        expected_messages=[expected_image],
        queue_url=queue_url
    )


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


def _assert_sqs_has_messages(expected_messages, queue_url):
    sqs_client = boto3.client('sqs')
    messages = sqs_client.receive_message(
        QueueUrl=queue_url,
        MaxNumberOfMessages=len(expected_messages)
    )

    assert len(messages['Messages']) == len(expected_messages)

    for msg, expected_msg in zip(messages['Messages'], expected_messages):
        body = msg['Body']
        inner_msg = json.loads(body)['Message']
        assert json.loads(json.loads(inner_msg)['default']) == expected_msg
