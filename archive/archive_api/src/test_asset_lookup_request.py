# -*- encoding: utf-8 -*-

import json

from asset_lookup_request import (
    asset_lookup_request
)


def test_looks_up_manifest(dynamodb_client, dynamodb_resource, s3_client, bucket_asset_lookup, table_name_asset_lookup):
    manifest_id = 'b12345678x'
    stored_manifest = {'manifest': manifest_id}

    given_manifest_in_vhs(
        manifest_id,
        stored_manifest,
        dynamodb_client,
        s3_client,
        bucket_asset_lookup,
        table_name_asset_lookup
    )

    response = asset_lookup_request(
        dynamodb_resource,
        table_name_asset_lookup,
        s3_client,
        bucket_asset_lookup,
        manifest_id
    )

    assert response == stored_manifest


def run_around_tests(dynamodb_client, s3_client, bucket_asset_lookup, table_name_asset_lookup):
    s3_client.create_bucket(Bucket=bucket_asset_lookup)

    create_table(dynamodb_client, table_name_asset_lookup)
    yield
    dynamodb_client.delete_table(TableName=table_name_asset_lookup)

    objects = s3_client.list_objects(Bucket=bucket_asset_lookup)
    for content in objects['Contents']:
        s3_client.delete_object(Bucket=bucket_asset_lookup, Key=content['Key'])
    s3_client.delete_bucket(Bucket=bucket_asset_lookup)


def create_table(dynamodb_client, table_name_asset_lookup):
    try:
        dynamodb_client.create_table(
            TableName=table_name_asset_lookup,
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
        dynamodb_client.get_waiter('table_exists').wait(TableName=table_name_asset_lookup)
    except dynamodb_client.exceptions.ResourceInUseException:
        pass


def given_manifest_in_vhs(manifest_id, stored_manifest, dynamodb_client, s3_client, bucket_asset_lookup, table_name_asset_lookup):
    key = manifest_id
    s3_client.put_object(Bucket=bucket_asset_lookup, Key=key, Body=json.dumps(stored_manifest))

    dynamodb_client.put_item(
        TableName=table_name_asset_lookup,
        Item={
            'id': {
                'S': manifest_id
            },
            's3key': {
                'S': key
            }
        }
    )
