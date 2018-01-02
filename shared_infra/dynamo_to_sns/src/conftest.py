# -*- encoding: utf-8 -*-

import os

import boto3
from moto import mock_ec2, mock_autoscaling, mock_ecs, mock_sns, mock_sqs
import pytest


def pytest_runtest_setup(item):
    set_region()


@pytest.fixture()
def set_region():
    # Without this, boto3 is complaining about not having a region defined
    # in tests (despite one being set in the Travis env variables and passed
    # into the image).
    # TODO: Investigate this properly.
    boto3.setup_default_session(region_name='eu-west-1')


@pytest.fixture()
def moto_start(set_region):
    mock_sns().start()
    mock_sqs().start()
    yield
    mock_sns().stop()
    mock_sqs().stop()


@pytest.fixture()
def topic_arn(moto_start):
    fake_sns_client = boto3.client('sns')
    topic_name = "test-topic"

    print(f'Creating topic {topic_name}')
    fake_sns_client.create_topic(Name=topic_name)
    response = fake_sns_client.list_topics()

    topic_arn = response['Topics'][0]['TopicArn']

    # DynamoToSNS reads its topic ARN from the environment, so we'll just
    # set it here.
    os.environ.update({'TOPIC_ARN': topic_arn})

    yield topic_arn


@pytest.fixture()
def queue_url(set_region, moto_start, topic_arn):
    fake_sns_client = boto3.client('sns')
    fake_sqs_client = boto3.client('sqs')
    queue_name = "test-queue"

    queue = fake_sqs_client.create_queue(QueueName=queue_name)

    fake_sns_client.subscribe(
        TopicArn=topic_arn,
        Protocol="sqs",
        Endpoint=f"arn:aws:sqs:eu-west-1:123456789012:{queue_name}"
    )
    yield queue['QueueUrl']
