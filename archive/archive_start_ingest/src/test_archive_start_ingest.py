# -*- encoding: utf-8 -*-

import os
from uuid import UUID

import pytest

import archive_start_ingest as start_ingest


TABLE_NAME = 'archive-storage-progress-table'


def test_post_sends_location_to_sns(sns_client, topic_arn):
    request = ingest_request(upload_url='s3://wellcomecollection-assets-archive-ingest/test-bag.zip')

    response = start_ingest.main(event=request, sns_client=sns_client)

    id = str(UUID(response['id']))
    assert id

    assert response['location'] == f"/ingests/{id}"

    messages = sns_client.list_messages()
    assert len(messages) == 1
    assert messages[0][':message'] == {
        'archiveRequestId': id,
        'bagLocation': {
            'namespace': 'wellcomecollection-assets-archive-ingest',
            'key': 'test-bag.zip'
        }
    }


def test_sends_request_to_sns_with_callback(sns_client, topic_arn):
    request = ingest_request(upload_url='s3://wellcomecollection-assets-archive-ingest/test-bag.zip',
                             callback_url='https://workflow.wellcomecollection.org/callback?id=b1234567')

    response = start_ingest.main(event=request, sns_client=sns_client)

    actual_id = str(UUID(response['id']))
    assert actual_id

    messages = sns_client.list_messages()
    assert len(messages) == 1
    assert messages[0][':message'] == {
        'archiveRequestId': actual_id,
        'bagLocation': {
            'namespace': 'wellcomecollection-assets-archive-ingest',
            'key': 'test-bag.zip'
        },
        'callbackUrl': 'https://workflow.wellcomecollection.org/callback?id=b1234567'
    }


def test_invalid_url_fails(sns_client):
    request = ingest_request('invalidUrl')

    with pytest.raises(ValueError, match="\[BadRequest\] Unrecognised url scheme: invalid"):
        start_ingest.main(event=request, sns_client=sns_client)

    assert len(sns_client.list_messages()) == 0


def test_missing_url_fails(sns_client):
    request = {"body": {'unknownKey': 'aValue'}}

    with pytest.raises(KeyError,
                       match="\[BadRequest\] Invalid request missing 'uploadUrl' in {'unknownKey': 'aValue'}"):
        start_ingest.main(event=request, sns_client=sns_client)

    assert len(sns_client.list_messages()) == 0


def test_invalid_json_fails(sns_client):
    request = {"body": "not_json"}

    with pytest.raises(TypeError, match="\[BadRequest\] Invalid request not json: not_json"):
        start_ingest.main(event=request, sns_client=sns_client)

    assert len(sns_client.list_messages()) == 0


def ingest_request(upload_url, callback_url=None):
    body = {
        'uploadUrl': upload_url
    }
    if callback_url:
        body['callbackUrl'] = callback_url
    return {
        'body': body,
        'path': '/ingests/'
    }


@pytest.yield_fixture(autouse=True)
def run_around_tests(dynamodb_client):
    os.environ['TABLE_NAME'] = TABLE_NAME

    create_table(dynamodb_client, TABLE_NAME)
    yield
    dynamodb_client.delete_table(TableName=TABLE_NAME)


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
