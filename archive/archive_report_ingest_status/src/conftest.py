# -*- encoding: utf-8

import os
import random

import pytest


@pytest.fixture()
def table_name():
    return 'archive_report_ingest_status--table-%d' % random.randint(0, 10000)


@pytest.yield_fixture(autouse=True)
def run_around_tests(dynamodb_client, table_name):
    os.environ.update({'TABLE_NAME': table_name})
    create_table(dynamodb_client, table_name)
    yield
    dynamodb_client.delete_table(TableName=table_name)

    try:
        del os.environ['TABLE_NAME']
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
