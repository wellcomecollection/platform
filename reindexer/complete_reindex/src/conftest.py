# -*- encoding: utf-8

import os

import pytest


@pytest.fixture(scope='session')
def table_name():
    name = 'ReindexShardTracker'
    os.environ.update({'TABLE_NAME': name})
    return name


@pytest.fixture(scope='session')
def reindex_shard_tracker_table(dynamodb_client, table_name):
    table = dynamodb_client.create_table(
        TableName=table_name,
        KeySchema=[
            {
                'AttributeName': 'shardId',
                'KeyType': 'HASH'
            }
        ],
        AttributeDefinitions=[
            {
                'AttributeName': "shardId",
                'AttributeType': "S"
            }
        ],
        ProvisionedThroughput={
            'ReadCapacityUnits': 1,
            'WriteCapacityUnits': 1
        }
    )

    return table_name
