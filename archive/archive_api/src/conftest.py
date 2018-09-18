# -*- encoding: utf-8

import os
import random
import uuid

import pytest


@pytest.fixture
def client(dynamodb_resource, table_name, sns_client, topic_arn):
    os.environ.update({'TABLE_NAME': table_name})

    from archive_api import app

    # @@AWLC: I feel like there should be a better way to configure the
    # test app config than setting it directly here.  For some reason it's
    # ignoring the ``app.config.from_object("config")`` call in the main file,
    # and I don't have time to debug it more thoroughly.
    app.config['dynamodb_table_name'] = table_name
    app.config['dynamodb_resource'] = dynamodb_resource
    app.config['sns_client'] = sns_client
    app.config['sns_topic_arn'] = topic_arn

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
