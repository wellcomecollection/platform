# -*- encoding: utf-8

import os
import random
import uuid

import pytest


@pytest.fixture
def client(dynamodb_resource, s3_client, table_name, sns_client, topic_arn, table_name_bag, bucket_bag):
    from archive_api import app
    app.config['DYNAMODB_TABLE_NAME'] = table_name
    app.config['DYNAMODB_RESOURCE'] = dynamodb_resource
    app.config['SNS_CLIENT'] = sns_client
    app.config['SNS_TOPIC_ARN'] = topic_arn
    app.config['S3_CLIENT'] = s3_client
    app.config['BAG_VHS_TABLE_NAME'] = table_name_bag
    app.config['BAG_VHS_BUCKET_NAME'] = bucket_bag

    yield app.test_client()


@pytest.fixture
def guid():
    return str(uuid.uuid4())


@pytest.fixture()
def table_name(dynamodb_client):
    dynamodb_table_name = 'report_ingest_status--table-%d' % random.randint(0, 10000)
    os.environ.update({'TABLE_NAME': dynamodb_table_name})
    create_table(dynamodb_client, dynamodb_table_name)
    yield dynamodb_table_name
    dynamodb_client.delete_table(TableName=dynamodb_table_name)
    try:
        del os.environ['TABLE_NAME']
    except KeyError:
        pass


@pytest.fixture()
def table_name_bag(dynamodb_client):
    dynamodb_table_name = 'bag--table-%d' % random.randint(0, 10000)
    os.environ.update({'BAG_VHS_TABLE_NAME': dynamodb_table_name})
    create_table(dynamodb_client, dynamodb_table_name)
    yield dynamodb_table_name
    dynamodb_client.delete_table(TableName=dynamodb_table_name)
    try:
        del os.environ['BAG_VHS_TABLE_NAME']
    except KeyError:
        pass


def create_table(dynamodb_client, table_name):
    try:
        dynamodb_client.create_table(
            TableName=table_name,
            KeySchema=[
                {
                    'AttributeName': 'id',
                    'KeyType': 'HASH'
                }
            ],
            AttributeDefinitions=[
                {
                    'AttributeName': 'id',
                    'AttributeType': 'S'
                }
            ],
            ProvisionedThroughput={
                'ReadCapacityUnits': 1,
                'WriteCapacityUnits': 1
            }
        )
        dynamodb_client.get_waiter('table_exists').wait(TableName=table_name)
    except dynamodb_client.exceptions.ResourceInUseException:
        pass


@pytest.fixture
def bag_id():
    return str(uuid.uuid4())


@pytest.fixture
def bucket_bag(s3_client):
    bucket_name = 'test-python-bag-bucket-%d' % random.randint(0, 10000)
    os.environ.update({'BAG_VHS_BUCKET_NAME': bucket_name})
    s3_client.create_bucket(Bucket=bucket_name)
    yield bucket_name
    try:
        del os.environ['BAG_VHS_BUCKET_NAME']
    except KeyError:
        pass
