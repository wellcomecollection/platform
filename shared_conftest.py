# -*- encoding: utf-8 -*-
"""
Global py.test configuration for our Lambdas.
"""

import os
import random
import string

import boto3
import pytest
import requests


def pytest_runtest_setup(item):
    # Set a default region before we start running tests.
    #
    # Without this line, boto3 complains about not having a region defined
    # (despite one being passed in the Travis env variables/local config).
    # TODO: Investigate this properly.
    boto3.setup_default_session(region_name="eu-west-1")


@pytest.fixture
def random_alpha():
    return "".join(random.choice(string.ascii_lowercase) for _ in range(10))


@pytest.fixture(scope="session")
def docker_compose_file(pytestconfig):
    root_docker_compose = pytestconfig.rootdir.join("docker-compose.yml")
    src_docker_compose = pytestconfig.rootdir.join("src", "docker-compose.yml")

    if root_docker_compose.exists():
        return root_docker_compose
    elif src_docker_compose.exists():
        return src_docker_compose
    else:
        assert False, "Cannot find docker-compose file!"


def _is_responsive(endpoint_url, condition):
    def is_responsive():
        try:
            resp = requests.get(endpoint_url)
            if condition(resp):
                return True
        except requests.exceptions.ConnectionError:
            return False

    return is_responsive


@pytest.fixture(scope="session")
def dynamodb_client(docker_services, docker_ip):
    endpoint_url = f'http://{docker_ip}:{docker_services.port_for("dynamodb", 8000)}'

    docker_services.wait_until_responsive(
        timeout=5.0,
        pause=0.1,
        check=_is_responsive(
            endpoint_url,
            lambda r: (
                r.status_code == 400
                and r.json()["__type"]
                == "com.amazonaws.dynamodb.v20120810#MissingAuthenticationToken"
            ),
        ),
    )

    yield boto3.client(
        "dynamodb",
        aws_access_key_id="testAccessKey",
        aws_secret_access_key="testSecretAccessKey",
        endpoint_url=endpoint_url,
    )


@pytest.fixture(scope="session")
def dynamodb_resource(docker_services, docker_ip):
    endpoint_url = f'http://{docker_ip}:{docker_services.port_for("dynamodb", 8000)}'

    docker_services.wait_until_responsive(
        timeout=5.0,
        pause=0.1,
        check=_is_responsive(
            endpoint_url,
            lambda r: (
                r.status_code == 400
                and r.json()["__type"]
                == "com.amazonaws.dynamodb.v20120810#MissingAuthenticationToken"
            ),
        ),
    )

    yield boto3.resource(
        "dynamodb",
        aws_access_key_id="testAccessKey",
        aws_secret_access_key="testSecretAccessKey",
        endpoint_url=endpoint_url,
    )


@pytest.fixture(scope="session")
def s3_endpoint_url(docker_services, docker_ip):
    return f'http://{docker_ip}:{docker_services.port_for("s3", 8000)}'


@pytest.fixture(scope="session")
def s3_client(s3_endpoint_url, docker_services):
    docker_services.wait_until_responsive(
        timeout=10.0,
        pause=0.1,
        check=_is_responsive(
            s3_endpoint_url,
            lambda r: (r.status_code == 403 and "<Code>AccessDenied</Code>" in r.text),
        ),
    )

    # These credentials are required for the scality/s3server image we use.
    yield boto3.client(
        "s3",
        aws_access_key_id="accessKey1",
        aws_secret_access_key="verySecretKey1",
        endpoint_url=s3_endpoint_url,
    )


@pytest.fixture(scope="session")
def sqs_endpoint_url(docker_services, docker_ip):
    return f'http://{docker_ip}:{docker_services.port_for("sqs", 9324)}'


@pytest.fixture(scope="session")
def sqs_client(sqs_endpoint_url, docker_services):
    docker_services.wait_until_responsive(
        timeout=45.0,
        pause=0.1,
        check=_is_responsive(sqs_endpoint_url, lambda r: r.status_code == 404),
    )

    sqs_client = boto3.client("sqs", endpoint_url=sqs_endpoint_url)

    yield sqs_client


@pytest.fixture(scope="session")
def sns_endpoint_url(docker_services, docker_ip):
    return f'http://{docker_ip}:{docker_services.port_for("sns", 9292)}'


@pytest.fixture(scope="session")
def sns_client(sns_endpoint_url, docker_services):
    docker_services.wait_until_responsive(
        timeout=5.0,
        pause=0.1,
        check=_is_responsive(sns_endpoint_url, lambda r: r.status_code == 200),
    )

    client = boto3.client(
        "sns",
        aws_access_key_id="testAccessKey",
        aws_secret_access_key="testSecretAccessKey",
        endpoint_url=sns_endpoint_url,
    )

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
    #   :message: '{"foo": "bar"}'
    #   :topic_arn: arn:aws:sns:us-east-1:123456789012:id_minter
    #   :structure:
    #   :target_arn:
    #   :received_at: 2017-04-18 13:20:45.289912607 +00:00

    def list_messages():
        import json
        import yaml

        resp = requests.get(sns_endpoint_url)
        data = yaml.safe_load(resp.text)["messages"]

        for d in data:
            d[":message"] = json.loads(d[":message"])
            try:
                d[":message"] = json.loads(d[":message"]["default"])
            except KeyError:
                pass

        return data

    # We monkey-patch this method into the SNS client, so it's available
    # in tests.  Dynamism FTW.
    client.list_messages = list_messages

    yield client


@pytest.fixture
def topic_arn(sns_client, docker_services, docker_ip):
    """Creates an SNS topic, and yields the new topic ARN."""
    topic_name = "test-lambda-topic"

    resp = sns_client.create_topic(Name=topic_name)
    topic_arn = resp["TopicArn"]

    # Our Lambdas all read their topic ARN from the environment, so we
    # set it here.
    os.environ.update({"TOPIC_ARN": topic_arn})

    yield topic_arn

    del os.environ["TOPIC_ARN"]

    # This clears all the messages on the topic at the end of the test,
    # so the next test gets an empty topic.
    endpoint_url = f'http://{docker_ip}:{docker_services.port_for("sns", 9292)}'
    requests.delete(f"{endpoint_url}/messages")


@pytest.fixture
def queue_url(sqs_client):
    """
    Creates an SQS queue and yields the new queue URL.
    """
    queue_name = "test-lambda-queue"

    resp = sqs_client.create_queue(QueueName=queue_name)
    queue_url = resp["QueueUrl"]

    yield queue_url

    sqs_client.delete_queue(QueueUrl=queue_url)


@pytest.fixture
def bucket(s3_client):
    bucket_name = "test-python-bucket-%d" % random.randint(0, 10000)
    s3_client.create_bucket(Bucket=bucket_name)
    yield bucket_name


@pytest.fixture
def elasticsearch_hostname(docker_ip):
    return docker_ip


@pytest.fixture
def elasticsearch_url(docker_services, elasticsearch_hostname):
    return f'http://{elasticsearch_hostname}:{docker_services.port_for("elasticsearch", 9200)}'


@pytest.fixture
def elasticsearch_index(docker_services, elasticsearch_url, random_alpha):
    docker_services.wait_until_responsive(
        timeout=60.0,
        pause=0.1,
        check=_is_responsive(elasticsearch_url, lambda r: r.status_code == 401),
    )

    index_name = random_alpha
    resp = requests.put(
        f"{elasticsearch_url}/{index_name}", auth=("elastic", "changeme")
    )
    resp.raise_for_status()

    yield index_name
