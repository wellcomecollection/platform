# -*- encoding: utf-8 -*-
"""
Global py.test configuration for our Lambdas.
"""

import os

import boto3
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


def _is_responsive(endpoint_url, condition):
    def is_responsive():
        try:
            resp = requests.get(endpoint_url)
            if condition(resp):
                return True
        except requests.exceptions.ConnectionError:
            return False

    return is_responsive


@pytest.fixture(scope='session')
def dynamodb_client(docker_services, docker_ip):
    endpoint_url = (
        f'http://{docker_ip}:{docker_services.port_for("dynamodb", 8000)}'
    )

    docker_services.wait_until_responsive(
        timeout=5.0, pause=0.1,
        check=_is_responsive(
            endpoint_url,
            lambda r: (
                r.status_code == 400 and
                r.json()['__type'] == 'com.amazonaws.dynamodb.v20120810#MissingAuthenticationToken'
            )
        )
    )

    yield boto3.client('dynamodb', endpoint_url=endpoint_url)


@pytest.fixture(scope='session')
def dynamodb_resource(docker_services, docker_ip):
    endpoint_url = (
        f'http://{docker_ip}:{docker_services.port_for("dynamodb", 8000)}'
    )

    docker_services.wait_until_responsive(
        timeout=5.0, pause=0.1,
        check=_is_responsive(
            endpoint_url,
            lambda r: (
                    r.status_code == 400 and
                    r.json()['__type'] == 'com.amazonaws.dynamodb.v20120810#MissingAuthenticationToken'
            )
        )
    )

    yield boto3.resource('dynamodb', endpoint_url=endpoint_url)


@pytest.fixture(scope='session')
def s3_client(docker_services, docker_ip):
    endpoint_url = (
        f'http://{docker_ip}:{docker_services.port_for("s3", 8000)}'
    )

    docker_services.wait_until_responsive(
        timeout=10.0, pause=0.1,
        check=_is_responsive(
            endpoint_url, lambda r: (
                r.status_code == 403 and
                '<Code>AccessDenied</Code>' in r.text)
        )
    )

    yield boto3.client(
        's3',
        aws_access_key_id='accessKey1',
        aws_secret_access_key='verySecretKey1',
        endpoint_url=endpoint_url
    )


@pytest.fixture(scope='session')
def sqs_client(docker_services, docker_ip):
    endpoint_url = (
        f'http://{docker_ip}:{docker_services.port_for("sqs", 9324)}'
    )

    docker_services.wait_until_responsive(
        timeout=5.0, pause=0.1,
        check=_is_responsive(endpoint_url, lambda r: r.status_code == 404)
    )

    yield boto3.client('sqs', endpoint_url=endpoint_url)


@pytest.fixture(scope='session')
def sns_client(docker_services, docker_ip):
    endpoint_url = (
        f'http://{docker_ip}:{docker_services.port_for("sns", 9292)}'
    )

    docker_services.wait_until_responsive(
        timeout=5.0, pause=0.1,
        check=_is_responsive(endpoint_url, lambda r: r.status_code == 200)
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
def topic_arn(sns_client, docker_services, docker_ip):
    """Creates an SNS topic in moto, and yields the new topic ARN."""
    topic_name = 'test-lambda-topic'

    resp = sns_client.create_topic(Name=topic_name)
    topic_arn = resp['TopicArn']

    # Our Lambdas all read their topic ARN from the environment, so we
    # set it here.
    os.environ.update({'TOPIC_ARN': topic_arn})

    yield topic_arn

    # This clears all the messages on the topic at the end of the test,
    # so the next test gets an empty topic.
    endpoint_url = (
        f'http://{docker_ip}:{docker_services.port_for("sns", 9292)}'
    )
    requests.delete(f'{endpoint_url}/messages')


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
