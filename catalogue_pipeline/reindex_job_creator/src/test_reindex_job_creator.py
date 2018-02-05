# -*- encoding: utf-8 -*-

import json
import time

import boto3
import pytest

from reindex_job_creator import main


def _wrap_single_image(image):
    return {
        'EventSource': 'aws:sns',
        'EventVersion': '1.0',
        'EventSubscriptionArn': 'arn:aws:sns:eu-west-1:1234567890:reindex_shard_tracker_updates:667a974d-d129-436a-a277-209604e31a5f',
        'Sns': {
            'Type': 'Notification',
            'MessageId': 'bcae81e8-6124-5e11-8cf0-d31a17b9ff4c',
            'TopicArn': 'arn:aws:sns:eu-west-1:1234567890:reindex_shard_tracker_updates',
            'Subject': 'default-subject',
            'Message': json.dumps(image),
            'Timestamp': '2018-02-05T15:12:29.459Z',
            'SignatureVersion': '1',
            'Signature': '<SNIP>',
            'SigningCertUrl': '<SNUP>',
            'MessageAttributes': {}
        }
    }


def _wrap(image):
    """Given an image from a DynamoDB table, wrap it in an SNS event."""
    return {
        'Records': [_wrap_single_image(image)]
    }


def test_higher_requested_version_triggers_job(queue_url):
    """
    If the DynamoDB table is updated so that desiredVersion > currentVersion,
    a job is sent to SQS.
    """
    image = {
        'shardId': 'example/ge',
        'currentVersion': 2,
        'desiredVersion': 3,
    }

    main(event=_wrap(image), _ctxt=None)

    sqs_client = boto3.client('sqs')
    messages = sqs_client.receive_message(
        QueueUrl=queue_url,
        MaxNumberOfMessages=1
    )
    message_body = messages['Messages'][0]['Body']
    inner_message = json.loads(message_body)['Message']
    parsed_message = json.loads(json.loads(inner_message)['default'])

    assert parsed_message == {
        'shardId': 'example/ge',
        'desiredVersion': 3,
    }


@pytest.mark.parametrize('current_version', [3, 4])
def test_lower_or_equal_requested_version_triggers_job(
    queue_url, current_version
):
    """
    If the DynamoDB table is updated so that desiredVersion <= currentVersion,
    nothing is sent to SQS.
    """
    image = {
        'shardId': 'example/leq',
        'currentVersion': current_version,
        'desiredVersion': 3,
    }

    main(event=_wrap(image), _ctxt=None)

    # We wait several seconds -- not ideal, but gives us some guarantee that
    # nothing is going to happen, and not just that we're checking the queue
    # before anything has happened!
    time.sleep(1)

    sqs_client = boto3.client('sqs')
    messages = sqs_client.receive_message(
        QueueUrl=queue_url,
        MaxNumberOfMessages=1
    )
    assert 'Messages' not in messages


def test_multiple_updates_trigger_jobs(queue_url):
    """
    If the DynamoDB table is updated so that desiredVersion > currentVersion,
    a job is sent to SQS.
    """
    images = [
        {
            'shardId': 'example/ge',
            'currentVersion': 5,
            'desiredVersion': 6,
        },
        {
            'shardId': 'example/eq',
            'currentVersion': 5,
            'desiredVersion': 5,
        },
        {
            'shardId': 'example/le',
            'currentVersion': 5,
            'desiredVersion': 3,
        },
        {
            'shardId': 'example/ge2',
            'currentVersion': 5,
            'desiredVersion': 7,
        },
    ]

    event = {
        'Records': [_wrap_single_image(img) for img in images]
    }

    main(event=event, _ctxt=None)

    sqs_client = boto3.client('sqs')
    messages = sqs_client.receive_message(
        QueueUrl=queue_url,
        MaxNumberOfMessages=4
    )

    assert len(messages['Messages']) == 2

    message_body = messages['Messages'][0]['Body']
    inner_message = json.loads(message_body)['Message']
    parsed_message = json.loads(json.loads(inner_message)['default'])

    assert parsed_message == {
        'shardId': 'example/ge',
        'desiredVersion': 6,
    }

    message_body = messages['Messages'][1]['Body']
    inner_message = json.loads(message_body)['Message']
    parsed_message = json.loads(json.loads(inner_message)['default'])

    assert parsed_message == {
        'shardId': 'example/ge2',
        'desiredVersion': 7,
    }
