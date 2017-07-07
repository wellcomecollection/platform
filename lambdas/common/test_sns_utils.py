from datetime import datetime
import json

import boto3
from moto import mock_sns, mock_sqs

import sns_utils


@mock_sns
@mock_sqs
def test_publish_sns_message():
    session = boto3.Session()
    region = session.region_name
    boto3.setup_default_session(region_name=region)

    topic_name = "test-topic"

    client = boto3.client('sns')
    client.create_topic(Name=topic_name)

    sqs_client = boto3.client('sqs')
    queue_name = "test-queue"
    queue = sqs_client.create_queue(QueueName=queue_name)

    response = client.list_topics()
    topic_arn = response["Topics"][0]['TopicArn']

    client.subscribe(
        TopicArn=topic_arn,
        Protocol="sqs",
        Endpoint=f"arn:aws:sqs:eu-west-1:123456789012:{queue_name}"
    )

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
        QueueUrl=queue['QueueUrl'],
        MaxNumberOfMessages=1
    )

    message_body = messages['Messages'][0]['Body']

    inner_message = json.loads(message_body)['default']
    actual_decoded_message = json.loads(inner_message)

    assert actual_decoded_message == expected_decoded_message
