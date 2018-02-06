# -*- encoding: utf-8 -*-

import json
import os

import boto3
from botocore.exceptions import ClientError
from moto import mock_dynamodb2
import pytest

from complete_reindex import _run, _process_reindex_tracker_update_job, _update_versioned_item

def _create_table(dynamodb, table_name):
    dynamodb.create_table(
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


@pytest.fixture
def reindex_shard_tracker_table():
    os.environ = {
        "AWS_ACCESS_KEY_ID": "fake",
        "AWS_SECRET_ACCESS_KEY": "fake"
    }

    dynamodb = boto3.resource('dynamodb', endpoint_url='http://dynamodb:8000')
    dynamodb_client = boto3.client('dynamodb')

    table_name = "ReindexShardTracker"

    try:
        _create_table(dynamodb, table_name)
    except dynamodb_client.exceptions.ResourceInUseException:
        table = dynamodb.Table(table_name)
        table.delete()
        _create_table(dynamodb, table_name)
        pass

    table = dynamodb.Table(table_name)

    yield table

    table.delete()

shard_id = "shard_id"
current_version = 1
desired_version = 2

example_message = {
    "shardId": shard_id,
    "completedReindexVersion": desired_version
}

encoded_example_message = json.dumps(example_message)

example_event = {
    "Records": [
        {
            "EventVersion": "1.0",
            "EventSubscriptionArn": "eventsubscriptionarn",
            "EventSource": "aws:sns",
            "Sns": {
                "SignatureVersion": "1",
                "Timestamp": "1970-01-01T00:00:00.000Z",
                "Signature": "EXAMPLE",
                "SigningCertUrl": "EXAMPLE",
                "MessageId": "95df01b4-ee98-5cb9-9903-4c221d41eb5e",
                "Message": encoded_example_message,
                "MessageAttributes": {},
                "Type": "Notification",
                "UnsubscribeUrl": "EXAMPLE",
                "TopicArn": "topicarn",
                "Subject": "TestInvoke"
            }
        }
    ]
}


def test_request_reindex(reindex_shard_tracker_table):
    table = reindex_shard_tracker_table

    os.environ = {
        "TABLE_NAME": table.table_name
    }

    table.put_item(
        Item={
            'shardId': shard_id,
            'currentVersion': current_version,
            'desiredVersion': desired_version,
            'version': 0,
        }
    )

    _run(table, example_event)

    dynamodb_response = table.get_item(Key={'shardId': shard_id})

    print(dynamodb_response['Item'])

    assert dynamodb_response['Item']['shardId'] == shard_id
    assert dynamodb_response['Item']['currentVersion'] == desired_version
    assert dynamodb_response['Item']['desiredVersion'] == desired_version
    assert dynamodb_response['Item']['version'] == 1


def test_request_reindex_does_not_revert_current_version_update(reindex_shard_tracker_table):
    table = reindex_shard_tracker_table

    os.environ = {
        "TABLE_NAME": table.table_name
    }

    updated_version = 4

    table.put_item(
        Item={
            'shardId': shard_id,
            'currentVersion': updated_version,
            'desiredVersion': updated_version,
            'version': 0,
        }
    )

    _run(table, example_event)

    dynamodb_response = table.get_item(Key={'shardId': shard_id})

    print(dynamodb_response['Item'])

    assert dynamodb_response['Item']['shardId'] == shard_id
    assert dynamodb_response['Item']['currentVersion'] == updated_version
    assert dynamodb_response['Item']['desiredVersion'] == updated_version
    assert dynamodb_response['Item']['version'] == 0


def test_request_reindex_throws_conditional_update_exception(reindex_shard_tracker_table):
    table = reindex_shard_tracker_table

    os.environ = {
        "TABLE_NAME": table.table_name
    }

    table.put_item(
        Item={
            'shardId': shard_id,
            'currentVersion': 1,
            'desiredVersion': 3,
            'version': 0,
        }
    )

    job = _process_reindex_tracker_update_job(table, {
        "shardId": shard_id,
        "completedReindexVersion": 3
    })

    table.put_item(
        Item={
            'shardId': shard_id,
            'currentVersion': 2,
            'desiredVersion': 2,
            'version': 1,
        }
    )

    with pytest.raises(ClientError) as e:
        _update_versioned_item(table, job['dynamo_item'], job['desired_item'])
        assert e.value.response['Error']['Code'] != 'ConditionalCheckFailedException'

