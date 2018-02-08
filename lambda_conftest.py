# -*- encoding: utf-8 -*-
"""
Global py.test configuration for our Lambdas.
"""

import os

import boto3
from moto import mock_sns, mock_sqs
import pytest
import requests


def pytest_runtest_setup(item):

    # Set a default region before we start running tests.
    #
    # Without this line, boto3 complains about not having a region defined
    # (despite one being passed in the Travis env variables/local config).
    # TODO: Investigate this properly.
    boto3.setup_default_session(region_name='eu-west-1')


@pytest.fixture(scope='session')
def docker_compose_file(pytestconfig):
    return os.path.join(str(pytestconfig.rootdir), 'docker-compose.yml')


def _dynamodb_is_responsive(endpoint_url):
    """
    Check if our DynamoDB container has started.  We GET the / path,
    and check we get a 400 error that indicates we need auth!
    """
    try:
        resp = requests.get(endpoint_url)
        if (
            resp.status_code == 400 and
            resp.json()['__type'] == 'com.amazonaws.dynamodb.v20120810#MissingAuthenticationToken'
        ):
            return True
    except requests.exceptions.ConnectionError:
        return False


@pytest.fixture(scope='session')
def dynamodb_client(docker_services, docker_ip):
    endpoint_url = (
        f'http://{docker_ip}:{docker_services.port_for("dynamodb", 8000)}'
    )

    docker_services.wait_until_responsive(
       timeout=5.0, pause=0.1,
       check=lambda: _dynamodb_is_responsive(endpoint_url)
    )

    yield boto3.client('dynamodb', endpoint_url=endpoint_url)


def _s3_is_responsive(endpoint_url):
    """
    Check if our S3 container has started.  We GET the / path,
    and check we get a 403 error that indicates we need auth!
    """
    try:
        resp = requests.get(endpoint_url)
        if (
            resp.status_code == 403 and
            '<Code>AccessDenied</Code>' in resp.text
        ):
            return True
    except requests.exceptions.ConnectionError:
        return False


@pytest.fixture(scope='session')
def s3_client(docker_services, docker_ip):
    endpoint_url = (
        f'http://{docker_ip}:{docker_services.port_for("s3", 8000)}'
    )

    docker_services.wait_until_responsive(
       timeout=10.0, pause=0.1,
       check=lambda: _s3_is_responsive(endpoint_url)
    )

    yield boto3.client(
        's3',
        aws_access_key_id='accessKey1',
        aws_secret_access_key='verySecretKey1',
        endpoint_url=endpoint_url
    )


def _sqs_is_responsive(endpoint_url):
    """
    Check if our SQS container has started.  We GET the / path,
    and check we get a 404 error that indicates we're looking up a
    non-existent queue.
    """
    try:
        resp = requests.get(endpoint_url)
        if resp.status_code == 404:
            return True
    except requests.exceptions.ConnectionError:
        return False


@pytest.fixture(scope='session')
def sqs_client(docker_services, docker_ip):
    endpoint_url = (
        f'http://{docker_ip}:{docker_services.port_for("sqs", 9324)}'
    )

    docker_services.wait_until_responsive(
       timeout=5.0, pause=0.1,
       check=lambda: _sqs_is_responsive(endpoint_url)
    )

    yield boto3.client('sqs', endpoint_url=endpoint_url)


def _sns_is_responsive(endpoint_url):
    """
    Check if our SQS container has started.  We GET the / path,
    and check we get a 404 error that indicates we're looking up a
    non-existent queue.
    """
    try:
        resp = requests.get(endpoint_url)
        if resp.status_code == 200:
            return True
    except requests.exceptions.ConnectionError:
        return False


@pytest.fixture(scope='session')
def sns_client(docker_services, docker_ip):
    endpoint_url = (
        f'http://{docker_ip}:{docker_services.port_for("sns", 9292)}'
    )

    docker_services.wait_until_responsive(
       timeout=5.0, pause=0.1,
       check=lambda: _sns_is_responsive(endpoint_url)
    )

    client = boto3.client('sns', endpoint_url=endpoint_url)

    # This is a sample returned by the fake-sns implementation:
    # ---
    # topics:
    # - arn: arn:aws:sns:us-east-1:123456789012:es_ingest
    #   name: es_ingest
    # - arn: arn:aws:sns:us-east-1:123456789012:id_minter
    #   name: id_minter
    # messages:
    # - :id: acbca1e1-e3c5-4c74-86af-06a9418e8fe4
    #   :subject: Foo
    #   :message: '{"identifiers":[{"source":"Miro","sourceId":"MiroID","value":"1234"}],"title":"some
    #     image title","accessStatus":null}'
    #   :topic_arn: arn:aws:sns:us-east-1:123456789012:id_minter
    #   :structure:
    #   :target_arn:
    #   :received_at: 2017-04-18 13:20:45.289912607 +00:00

    def list_messages():
        import json
        import yaml

        resp = requests.get(f'{endpoint_url}')
        data = yaml.safe_load(resp.text)['messages']
        for d in data:
            d[':message'] = json.loads(json.loads(d[':message'])['default'])
        return data

    # We monkey-patch this method into the SNS client, so it's available
    # in tests.  Dynamism FTW.
    client.list_messages = list_messages

    yield client


@pytest.fixture
def topic_arn(sns_client):
    """Creates an SNS topic in moto, and yields the new topic ARN."""
    topic_name = 'test-lambda-topic'

    resp = sns_client.create_topic(Name=topic_name)
    topic_arn = resp['TopicArn']

    # Our Lambdas all read their topic ARN from the environment, so we
    # set it here.
    os.environ.update({'TOPIC_ARN': topic_arn})

    yield topic_arn


@pytest.fixture
def queue_url(sns_client, sqs_client, topic_arn):
    """
    Creates an SQS queue in moto, subscribes it to an SNS topic, and
    yields the new queue URL.
    """
    queue_name = 'test-lambda-queue'

    resp = sqs_client.create_queue(QueueName=queue_name)
    queue_url = resp['QueueUrl']

    sns_client.subscribe(
        TopicArn=topic_arn,
        Protocol='sqs',
        Endpoint=f'arn:aws:sqs:eu-west-1:123456789012:{queue_name}'
    )
    yield queue_url


@pytest.fixture
def moto_topic_arn():
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
def moto_queue_url(moto_topic_arn):
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
            TopicArn=moto_topic_arn,
            Protocol='sqs',
            Endpoint=f'arn:aws:sqs:eu-west-1:123456789012:{queue_name}'
        )
        yield queue_url
