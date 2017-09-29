# -*- encoding: utf-8 -*-

import pytest
import boto3
from moto import mock_sns, mock_sqs


def pytest_runtest_setup(item):
    set_region()


@pytest.fixture
def s3_put_event():
    """
    This is the S3 PUT event that the Lambda console uses as a test event.
    """
    return {"Records": [{
        "eventVersion": "2.0",
        "eventTime": "1970-01-01T00:00:00.000Z",
        "requestParameters": {"sourceIPAddress": "127.0.0.1"},
        "s3": {
            "configurationId": "testConfigRule",
            "object": {
                "eTag": "0123456789abcdef0123456789abcdef",
                "sequencer": "0A1B2C3D4E5F678901",
                "key": "metadata.json",
                "size": 1024
            },
            "bucket": {
                "arn": "arn:aws:s3:::mybucket",
                "name": "miro-data",
                "ownerIdentity": {"principalId": "EXAMPLE"}
            },
            "s3SchemaVersion": "1.0"
        },
        "responseElements": {
            "x-amz-id-2": "EXAMPLE123/5678abcdefghijklambdaisawesome/mnopqrstuvwxyzABCDEFGH",
            "x-amz-request-id": "EXAMPLE123456789"
        },
        "awsRegion": "us-east-1",
        "eventName": "ObjectCreated:Put",
        "userIdentity": {"principalId": "EXAMPLE"},
        "eventSource": "aws:s3"
    }]
    }


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
def image_sorter_sns_sqs(set_region, moto_start):
    fake_sns_client = boto3.client('sns')
    fake_sqs_client = boto3.client('sqs')

    cold_store_topic, cold_store_queue = _create_topic_and_queue(
        fake_sns_client, fake_sqs_client, "cold_store")

    tandem_vault_topic, tandem_vault_queue = _create_topic_and_queue(
        fake_sns_client, fake_sqs_client, "tandem_vault")

    catalogue_api_topic, catalogue_api_queue = _create_topic_and_queue(
        fake_sns_client, fake_sqs_client, "catalogue_api")

    none_topic, none_queue = _create_topic_and_queue(
        fake_sns_client, fake_sqs_client, "none")

    digital_library_topic, digital_library_queue = _create_topic_and_queue(
        fake_sns_client, fake_sqs_client, "digital_library")

    print(cold_store_queue)

    yield {
        "cold_store": {
            "topic": cold_store_topic,
            "queue": cold_store_queue
        },
        "tandem_vault": {
            "topic": tandem_vault_topic,
            "queue": tandem_vault_queue
        },
        "catalogue_api": {
            "topic": catalogue_api_topic,
            "queue": catalogue_api_queue
        },
        "none": {
            "topic": none_topic,
            "queue": none_queue
        },
        "digital_library": {
            "topic": digital_library_topic,
            "queue": digital_library_queue
        }
    }
