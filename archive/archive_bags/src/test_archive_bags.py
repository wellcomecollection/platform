# -*- encoding: utf-8 -*-

import archive_bags
import json
import os
import pytest

VHS_TABLE_NAME = 'vhs-archive-manifests'
VHS_BUCKET_NAME = 'wellcomecollection-vhs-archive-manifests'


def test_looks_up_manifest(dynamodb_client, s3_client):
    manifest_id = 'b12345678x'
    stored_manifest = {'manifest': manifest_id}

    given_manifest_in_vhs(
        manifest_id,
        stored_manifest,
        dynamodb_client,
        s3_client
    )

    request = {'id': manifest_id}

    response = archive_bags.handler(
        request,
        None,
        dynamodb_client,
        s3_client
    )

    assert response == stored_manifest


@pytest.yield_fixture(autouse=True)
def run_around_tests(dynamodb_client, s3_client):
    os.environ['VHS_TABLE_NAME'] = VHS_TABLE_NAME
    os.environ['VHS_BUCKET_NAME'] = VHS_BUCKET_NAME

    s3_client.create_bucket(Bucket=VHS_BUCKET_NAME)

    create_table(dynamodb_client, VHS_TABLE_NAME)
    yield
    dynamodb_client.delete_table(TableName=VHS_TABLE_NAME)

    objects = s3_client.list_objects(Bucket=VHS_BUCKET_NAME)
    for content in objects['Contents']:
        s3_client.delete_object(Bucket=VHS_BUCKET_NAME, Key=content['Key'])
    s3_client.delete_bucket(Bucket=VHS_BUCKET_NAME)


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


def given_manifest_in_vhs(manifest_id, stored_manifest, dynamodb_client, s3_client):
    key = f"/archive/80/b22454408/{manifest_id}"
    s3_client.put_object(Bucket=VHS_BUCKET_NAME, Key=key, Body=json.dumps(stored_manifest))

    dynamodb_client.put_item(
        TableName=VHS_TABLE_NAME,
        Item={
            'id': {
                'S': manifest_id
            },
            's3key': {
                'S': key
            }
        }
    )
