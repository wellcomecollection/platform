# -*- encoding: utf-8

import os
import time

import pytest

from resharder import main


def test_ignores_already_resharded_row(
    dynamodb_client, source_data_table, s3_client, source_bucket
):
    item = _dynamodb_item(
        id='sierra/b1111111',
        s3key='sierra/b1111111.json',
        resharded=True
    )
    dynamodb_client.put_item(TableName=source_data_table, Item=item)

    event = _wrap(item)

    main(event=event, dynamodb_client=dynamodb_client, s3_client=s3_client)

    time.sleep(1)

    item = dynamodb_client.get_item(
        TableName=source_data_table,
        Key={'id': {'S': 'sierra/b1111111'}}
    )

    assert item['Item']['version']['N'] == '1'


def test_updates_old_row(
    dynamodb_client, source_data_table, s3_client, source_bucket_name, source_bucket
):
    item = _dynamodb_item(
        id='sierra/b2222223',
        s3key='sierra/b2222223/abc.json'
    )

    resp = dynamodb_client.put_item(TableName=source_data_table, Item=item)
    time.sleep(1)

    s3_client.put_object(
        Bucket=source_bucket_name,
        Key='sierra/b2222223/abc.json',
        Body=b'hello world'
    )

    event = _wrap(item)
    main(event=event, dynamodb_client=dynamodb_client, s3_client=s3_client)

    item = dynamodb_client.get_item(
        TableName=source_data_table,
        Key={'id': {'S': 'sierra/b2222223'}}
    )

    assert item['Item']['resharded']['BOOL'] == True
    assert item['Item']['version']['N'] == '2'

    print(s3_client.list_objects(Bucket=source_bucket_name))

    resp = s3_client.get_object(
        Bucket=source_bucket_name,
        Key='sierra/32/b2222223/abc.json'
    )
    body = resp['Body'].read()
    assert body == b'hello world'


@pytest.fixture(scope='session')
def source_bucket_name():
    name = 'test-resharder-bucket'
    os.environ.update({'S3_BUCKET': name})
    return name


@pytest.fixture(scope='session')
def source_bucket(s3_client, source_bucket_name):
    s3_client.create_bucket(Bucket=source_bucket_name)


@pytest.fixture(scope='session')
def table_name():
    name = 'test_resharder-table'
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
                    'id': {'S': 'sierra/b2097560'}
                },
                'NewImage': row,
                'SequenceNumber': '475718600000000014074466230',
                'SizeBytes': 207,
                'StreamViewType':
                    'NEW_AND_OLD_IMAGES'},
            'eventSourceARN': 'arn:aws:dynamodb:eu-west-1:1234567:table/SourceData/stream/2018-02-01T16:28:30.956'
        }]}


def _dynamodb_item(id, s3key, resharded=None, version=1):
    source_name, source_id = id.split('/')
    data = {
        'id': {'S': id},
        'sourceName': {'S': source_name},
        'sourceId': {'S': source_id},
        'version': {'N': str(version)},
        's3key': {'S': s3key},
    }

    if resharded is not None:
        data['resharded'] = {'BOOL': resharded}

    return data


def _dynamo_event(id, *args, **kwargs):
    data = _dynamodb_item(id, *args, **kwargs)
    return _wrap(data)
