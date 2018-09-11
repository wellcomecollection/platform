# -*- encoding: utf-8

import os
import random
import uuid

import pytest


@pytest.fixture
def client(dynamodb_resource, table_name):
    os.environ.update({'TABLE_NAME': table_name})

    # Set up test config.  We have to do this before we import the app,
    # so the monkey-patched resources are in place when the app is created.
    import config
    config.dynamodb_resource = dynamodb_resource

    from ingest_manager import app
    yield app.test_client()

    try:
        del os.environ['TABLE_NAME']
    except KeyError:
        pass


@pytest.fixture
def guid():
    return str(uuid.uuid4())


@pytest.fixture()
def table_name(dynamodb_client):
    table_name = 'report_ingest_status--table-%d' % random.randint(0, 10000)
    create_table(dynamodb_client, table_name)
    yield table_name
    dynamodb_client.delete_table(TableName=table_name)


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
