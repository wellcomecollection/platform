# -*- encoding: utf-8 -*-

import json
import pytest

from bags import (
    fetch_bag
)


def test_looks_up_bag(dynamodb_resource, s3_client, bucket_bag, table_name_bag):
    bag_id = 'b12345678x'
    stored_bag = {'id': bag_id}

    given_bag_in_vhs(
        bag_id,
        stored_bag,
        dynamodb_resource,
        s3_client,
        bucket_bag,
        table_name_bag
    )

    response = fetch_bag(
        dynamodb_resource,
        table_name_bag,
        s3_client,
        bucket_bag,
        bag_id
    )

    assert response == stored_bag


@pytest.yield_fixture(autouse=True)
def run_around_tests(dynamodb_client, s3_client, bucket_bag, table_name_bag):
    s3_client.create_bucket(Bucket=bucket_bag)

    yield

    objects = s3_client.list_objects(Bucket=bucket_bag)
    for content in objects['Contents']:
        s3_client.delete_object(Bucket=bucket_bag, Key=content['Key'])
    s3_client.delete_bucket(Bucket=bucket_bag)


def create_table(dynamodb_client, table_name_bag):
    try:
        dynamodb_client.create_table(
            TableName=table_name_bag,
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
        dynamodb_client.get_waiter('table_exists').wait(TableName=table_name_bag)
    except dynamodb_client.exceptions.ResourceInUseException:
        pass


def given_bag_in_vhs(bag_id, stored_bag, dynamodb_resource, s3_client, bucket_bag, table_name_bag):
    key = bag_id
    s3_client.put_object(Bucket=bucket_bag, Key=key, Body=json.dumps(stored_bag))

    table = dynamodb_resource.Table(table_name_bag)
    table.put_item(Item={
        'id': bag_id,
        'location': {
            'key': bag_id,
            'namespace': bucket_bag
        }
    })

