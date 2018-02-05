# -*- encoding: utf-8 -*-

import json
import time

import boto3
import pytest

from reindex_job_creator import main


def _wrap(image):
    """Given an image from a DynamoDB table, wrap it in an SNS event."""
    return {
        'Records': [
            {
                'eventID': '81659528846ddb9826c612c16043c2ea',
                'eventName': 'MODIFY',
                'eventVersion': '1.1',
                'eventSource': 'aws:dynamodb',
                'awsRegion': 'eu-west-1',
                'dynamodb': {
                    'NewImage': image,
                },
                'eventSourceARN': 'foo'
            }
        ]
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
