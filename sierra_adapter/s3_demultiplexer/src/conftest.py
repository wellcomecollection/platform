# -*- encoding: utf-8 -*-

import boto3
from moto import mock_sns, mock_sqs
import pytest


@pytest.fixture()
def moto_start():
    mock_sns().start()
    mock_sqs().start()
    yield
    mock_sns().stop()
    mock_sqs().stop()


@pytest.fixture()
def sns_sqs(moto_start):
    fake_sns_client = boto3.client('sns')
    fake_sqs_client = boto3.client('sqs')

    queue_name = "test-queue"
    topic_name = "test-topic"

    print(f"Creating topic {topic_name} and queue {queue_name}")

    fake_sns_client.create_topic(Name=topic_name)
    response = fake_sns_client.list_topics()
    topic_arn = response["Topics"][0]['TopicArn']

    queue = fake_sqs_client.create_queue(QueueName=queue_name)

    fake_sns_client.subscribe(
        TopicArn=topic_arn,
        Protocol="sqs",
        Endpoint=f"arn:aws:sqs:eu-west-1:123456789012:{queue_name}"
    )
    yield topic_arn, queue['QueueUrl']
