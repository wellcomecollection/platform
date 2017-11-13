# -*- encoding: utf-8 -*-

import pytest
import boto3
from moto import mock_sns, mock_sqs


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


def _create_topic_and_queue(sns_client, sqs_client, name):
    queue_name = f"test-{name}"
    topic_name = f"test-{name}"

    print(f"Creating topic {topic_name} and queue {queue_name}")

    sns_client.create_topic(Name=topic_name)
    response = sns_client.list_topics()

    topics = [topic for topic in response["Topics"] if name in topic["TopicArn"]]
    topic_arn = topics[0]['TopicArn']

    queue = sqs_client.create_queue(QueueName=queue_name)

    sns_client.subscribe(
        TopicArn=topic_arn,
        Protocol="sqs",
        Endpoint=f"arn:aws:sqs:eu-west-1:123456789012:{queue_name}"
    )

    return topic_arn, queue['QueueUrl']


@pytest.fixture()
def dynamo_to_sns_event_sns_sqs(set_region, moto_start):
    fake_sns_client = boto3.client('sns')
    fake_sqs_client = boto3.client('sqs')

    modify_topic, modify_queue = _create_topic_and_queue(
        fake_sns_client, fake_sqs_client, "modify")

    insert_topic, insert_queue = _create_topic_and_queue(
        fake_sns_client, fake_sqs_client, "insert")

    remove_topic, remove_queue = _create_topic_and_queue(
        fake_sns_client, fake_sqs_client, "remove")

    yield {
        "modify": {
            "topic": modify_topic,
            "queue": modify_queue
        },
        "insert": {
            "topic": insert_topic,
            "queue": insert_queue
        },
        "remove": {
            "topic": remove_topic,
            "queue": remove_queue
        }
    }
