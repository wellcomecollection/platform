from datetime import datetime
import json

import boto3

from utils import sns_utils


def test_publish_sns_message(sns_sqs):
    sqs_client = boto3.client('sqs')
    topic_arn, queue_url = sns_sqs

    test_message = {
        'string': 'a',
        'number': 1,
        'date':  datetime.strptime(
            'Jun 1 2005  1:33PM', '%b %d %Y %I:%M%p'
        )
    }

    expected_decoded_message = {
        'string': 'a',
        'number': 1,
        'date': '2005-06-01T13:33:00'
    }

    sns_utils.publish_sns_message(topic_arn, test_message)

    messages = sqs_client.receive_message(
        QueueUrl=queue_url,
        MaxNumberOfMessages=1
    )

    message_body = messages['Messages'][0]['Body']

    inner_message = json.loads(message_body)['default']
    actual_decoded_message = json.loads(inner_message)

    assert actual_decoded_message == expected_decoded_message
