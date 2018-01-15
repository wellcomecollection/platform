# -*- encoding: utf-8 -*-
"""
Global py.test configuration for our Lambdas.
"""

import os

import boto3
from moto import mock_sns, mock_sqs
import pytest


def pytest_runtest_setup(item):

    # Set a default region before we start running tests.
    #
    # Without this line, boto3 complains about not having a region defined
    # (despite one being passed in the Travis env variables/local config).
    # TODO: Investigate this properly.
    boto3.setup_default_session(region_name='eu-west-1')


@pytest.fixture
def topic_arn():
    """Creates an SNS topic in moto, and yields the new topic ARN."""
    with mock_sns():
        sns_client = boto3.client('sns')
        topic_name = 'test-lambda-topic'

        resp = sns_client.create_topic(Name=topic_name)
        topic_arn = resp['TopicArn']

        # Our Lambdas all read their topic ARN from the environment, so we
        # set it here.
        os.environ.update({'TOPIC_ARN': topic_arn})

        yield topic_arn


@pytest.fixture
def queue_url(topic_arn):
    """
    Creates an SQS queue in moto, subscribes it to an SNS topic, and
    yields the new queue URL.
    """
    with mock_sqs():
        sns_client = boto3.client('sns')
        sqs_client = boto3.client('sqs')
        queue_name = 'test-lambda-queue'

        resp = sqs_client.create_queue(QueueName=queue_name)
        queue_url = resp['QueueUrl']

        sns_client.subscribe(
            TopicArn=topic_arn,
            Protocol='sqs',
            Endpoint=f'arn:aws:sqs:eu-west-1:123456789012:{queue_name}'
        )
        yield queue_url
