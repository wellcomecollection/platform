# -*- encoding: utf-8 -*-

import os

import pytest

from reindex_shard_generator import main


@pytest.fixture(scope='session')
def table_name():
    name = 'test_reindex_shard_generator-table'
    os.environ.update({'TABLE_NAME': name})
    return name


@pytest.fixture(scope='session')
def sierra_source_name():
    os.environ.update({'SOURCE_NAME': 'sierra'})


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
    for _ in range(10):
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
            'eventID': 'a2ddf34215abc74d90efa4f70843ce4b',
            'eventName': 'MODIFY',
            'eventVersion': '1.1',
            'eventSource': 'aws:dynamodb',
            'awsRegion': 'eu-west-1',
            'dynamodb': {
                'ApproximateCreationDateTime': 1518608280.0,
                'Keys': {
                    'id': {'S': '2097560'}
                },
                'NewImage': row,
                'SequenceNumber': '475718600000000014074466230',
                'SizeBytes': 207,
                'StreamViewType':
                    'NEW_AND_OLD_IMAGES'},
            'eventSourceARN': 'arn:aws:dynamodb:eu-west-1:1234567:table/SourceData/stream/2018-02-01T16:28:30.956'
        }]}


def _dynamodb_item(id, reindex_shard=None, version=1):
    data = {
        'id': {'S': id},
        'version': {'N': str(version)},
    }
    if reindex_shard:
        data['reindexShard'] = {'S': reindex_shard}

    return data


def _dynamo_event(id, reindex_shard=None, version=1):
    data = _dynamodb_item(id, reindex_shard, version)
    return _wrap(data)


def test_adds_shard_and_reindex_version_to_new_record(dynamodb_client, source_data_table, sierra_source_name):
    dynamodb_client.put_item(
        TableName=source_data_table,
        Item=_dynamodb_item(id='1111111')
    )

    event = _dynamo_event(id='1111111')

    main(event=event, dynamodb_client=dynamodb_client)

    item = dynamodb_client.get_item(
        TableName=source_data_table,
        Key={'id': {'S': '1111111'}}
    )

    assert item['Item']['reindexShard']['S'] == '1119'
    assert 'reindexVersion' not in item['Item']


def test_updates_shard_on_old_record(dynamodb_client, source_data_table, sierra_source_name):
    dynamodb_client.put_item(
        TableName=source_data_table,
        Item=_dynamodb_item(id='2222222')
    )

    event = _dynamo_event(
        id='2222222',
        reindex_shard='oldReindexShard'
    )

    main(event=event, dynamodb_client=dynamodb_client)

    item = dynamodb_client.get_item(
        TableName=source_data_table,
        Key={'id': {'S': '2222222'}}
    )

    assert item['Item']['reindexShard']['S'] == '1414'


def test_does_nothing_if_shard_up_to_date(dynamodb_client, source_data_table, sierra_source_name):
    dynamodb_client.put_item(
        TableName=source_data_table,
        Item=_dynamodb_item(id='3333333',
                            reindex_shard='1709',
                            version=3)
    )

    event = _dynamo_event(
        id='3333333',
        reindex_shard='1709',
        version=3
    )

    main(event=event, dynamodb_client=dynamodb_client)

    item = dynamodb_client.get_item(
        TableName=source_data_table,
        Key={'id': {'S': '3333333'}}
    )
    assert item['Item']['version']['N'] == '3'


def test_does_not_override_record_with_old_version(dynamodb_client, source_data_table, sierra_source_name):
    dynamodb_client.put_item(
        TableName=source_data_table,
        Item=_dynamodb_item(id='3333333', version=3)
    )

    event = _dynamo_event(
        id='3333333',
        version=2
    )

    main(event=event, dynamodb_client=dynamodb_client)

    item = dynamodb_client.get_item(
        TableName=source_data_table,
        Key={'id': {'S': '3333333'}}
    )
    assert item['Item']['version']['N'] == '3'
    assert 'reindexShard' not in item['Item']


def test_does_nothing_if_it_receives_a_delete_update(dynamodb_client, source_data_table, sierra_source_name):
    dynamodb_client.delete_item(
        TableName=source_data_table,
        Key={'id': {'S': '3333333'}}
    )

    event = {
        'Records': [{
            "eventID": "7de3041dd709b024af6f29e4fa13d34c",
            "eventName": "REMOVE",
            "eventVersion": "1.1",
            "eventSource": "aws:dynamodb",
            "awsRegion": "us-west-2",
            "dynamodb": {
                'ApproximateCreationDateTime': 1518608280.0,
                'Keys': {
                    'id': {'S': '2097560'}
                },
                'SequenceNumber': '475718600000000014074466230',
                'SizeBytes': 207,
                'OldImage': _dynamodb_item('3333333', 3),
                'StreamViewType': 'NEW_AND_OLD_IMAGES'
            },
            "eventSourceARN": ''}]
    }

    main(event=event, dynamodb_client=dynamodb_client)

    item = dynamodb_client.get_item(
        TableName=source_data_table,
        Key={'id': {'S': '3333333'}}
    )
    assert 'Item' not in item
