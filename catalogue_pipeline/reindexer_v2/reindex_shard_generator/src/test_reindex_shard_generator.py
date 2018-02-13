# -*- encoding: utf-8 -*-

import json
import os

import pytest

from reindex_shard_generator import main


@pytest.fixture(scope='session')
def table_name():
    name = 'test_reindex_shard_generator-table'
    os.environ.update({'TABLE_NAME': name})
    return name


@pytest.fixture(scope='session')
def source_data_table(dynamodb_client, table_name):
    dynamodb_client.create_table(
        TableName=table_name,
        KeySchema=[
            {
                'AttributeName': 'id',
                'KeyType': 'HASH',
            }
        ],
        AttributeDefinitions=[
            {
                'AttributeName': 'id',
                'AttributeType': 'S',
            }
        ],
        ProvisionedThroughput={
            'ReadCapacityUnits': 1,
            'WriteCapacityUnits': 1,
        }
    )

    # Quoting from the boto3 docs:
    #
    #   CreateTable is an asynchronous operation. Upon receiving a CreateTable
    #   request, DynamoDB immediately returns a response with a TableStatus
    #   of CREATING . After the table is created, DynamoDB sets the
    #   TableStatus to ACTIVE . You can perform read and write operations
    #   only on an ACTIVE table.
    #
    #   You can use the DescribeTable action to check the table status.
    #
    # So we wait for the table to be created before yielding to the test.
    #
    # Link:  https://boto3.readthedocs.io/en/stable/reference/services/dynamodb.html#DynamoDB.Client.create_table
    for i in range(10):
        resp = dynamodb_client.describe_table(TableName=table_name)

        if resp['Table']['TableStatus'] == 'ACTIVE':
            break
        else:
            import time
            time.sleep(1)

    else:  # no break
        raise RuntimeError(
            f'Timed out waiting for table {table_name} to become ACTIVE'
        )

    return table_name


def _wrap(row):
    return {
        'Records': [{
            'EventSource': 'aws:sns',
            'EventVersion': '1.0',
            'EventSubscriptionArn': 'arn:aws:sns:eu-west-1:1234567890:reindex_table_updates:667a974d-d129-436a-a277-209604e31a5f',
            'Sns': {
                'Type': 'Notification',
                'MessageId': 'bcae81e8-6124-5e11-8cf0-d31a17b9ff4c',
                'TopicArn': 'arn:aws:sns:eu-west-1:1234567890:reindex_table_updates',
                'Subject': 'default-subject',
                'Message': json.dumps(row),
                'Timestamp': '2018-02-05T15:12:29.459Z',
                'SignatureVersion': '1',
                'Signature': '<SNIP>',
                'SigningCertUrl': '<SNUP>',
                'MessageAttributes': {}
            }
        }]
    }


def _dynamodb_item(id, reindex_shard=None, version=1):
    source_name, source_id = id.split('/')
    data = {
        'id': {'S': id},
        'sourceName': {'S': source_name},
        'sourceId': {'S': source_id},
        'version': {'N': str(version)},
    }
    if reindex_shard:
        data['reindexShard'] = {'S': reindex_shard}

    return data


def _sns_event(id, reindex_shard=None, version=1):
    source_name, source_id = id.split('/')
    data = {
        'id': id,
        'sourceName': source_name,
        'sourceId': source_id,
        'version': version
    }
    if reindex_shard:
        data['reindexShard'] = reindex_shard
    return _wrap(data)


def test_adds_shard_to_new_record(dynamodb_client, source_data_table):
    dynamodb_client.put_item(
        TableName=source_data_table,
        Item=_dynamodb_item(id='sierra/b1111111')
    )

    event = _sns_event(id='sierra/b1111111')

    main(event=event, dynamodb_client=dynamodb_client)

    item = dynamodb_client.get_item(
        TableName=source_data_table,
        Key={'id': {'S': 'sierra/b1111111'}}
    )
    assert item['Item']['reindexShard']['S'] == 'sierra/2a20'


def test_updates_shard_on_old_record(dynamodb_client, source_data_table):
    dynamodb_client.put_item(
        TableName=source_data_table,
        Item=_dynamodb_item(id='sierra/b2222222')
    )

    event = _sns_event(
        id='sierra/b2222222',
        reindex_shard='oldReindexShard'
    )

    main(event=event, dynamodb_client=dynamodb_client)

    item = dynamodb_client.get_item(
        TableName=source_data_table,
        Key={'id': {'S': 'sierra/b2222222'}}
    )
    assert item['Item']['reindexShard']['S'] == 'sierra/1cfc'


def test_does_nothing_if_shard_up_to_date(dynamodb_client, source_data_table):
    dynamodb_client.put_item(
        TableName=source_data_table,
        Item=_dynamodb_item(id='sierra/b3333333', version=3)
    )

    event = _sns_event(
        id='sierra/b3333333',
        reindex_shard='sierra/838d',
        version=3
    )

    main(event=event, dynamodb_client=dynamodb_client)

    item = dynamodb_client.get_item(
        TableName=source_data_table,
        Key={'id': {'S': 'sierra/b3333333'}}
    )
    assert item['Item']['version']['N'] == '3'


def test_does_not_override_record_with_old_version(dynamodb_client, source_data_table):
    dynamodb_client.put_item(
        TableName=source_data_table,
        Item=_dynamodb_item(id='sierra/b3333333', version=3)
    )

    event = _sns_event(
        id='sierra/b3333333',
        version=2
    )

    main(event=event, dynamodb_client=dynamodb_client)

    item = dynamodb_client.get_item(
        TableName=source_data_table,
        Key={'id': {'S': 'sierra/b3333333'}}
    )
    assert item['Item']['version']['N'] == '3'
    assert 'reindexShard' not in item['Item']