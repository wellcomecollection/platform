import boto3
from moto import mock_ec2, mock_autoscaling, mock_ecs, mock_sns, mock_sqs
import pytest


@pytest.fixture()
def set_region():
    # Need this otherwise boto complains about missing region
    # in sns_utils.pblish_sns_message when trying to create client
    # with sns = boto3.client('sns') (despite region being set with
    # the environment variable AWS_DEFAULT_REGION, which should be
    # read by default by boto)
    # Weirdly enough it doesn't complain in this file when it tries
    # to do the same thing.
    # After investigation this is not related to moto
    session = boto3.Session()
    region = session.region_name
    boto3.setup_default_session(region_name=region)


@pytest.fixture()
def moto_start(set_region):
    mock_autoscaling().start()
    mock_ec2().start()
    mock_ecs().start()
    mock_sns().start()
    mock_sqs().start()
    yield
    mock_autoscaling().stop()
    mock_ec2().stop()
    mock_ecs().stop()
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
