# -*- encoding: utf-8 -*-

import json
import os

from botocore.exceptions import ClientError
import pytest

from complete_reindex import _run, _process_reindex_tracker_update_job, _update_versioned_item, should_retry


@pytest.fixture
def reindex_shard_tracker_table(dynamodb_resource):
    table_name = "ReindexShardTracker"

    table = dynamodb_resource.create_table(
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

    yield table

    table.delete()


def _create_event(shard_id, current_version, desired_version):
    example_message = {
        "shardId": shard_id,
        "completedReindexVersion": desired_version
    }

    encoded_example_message = json.dumps(example_message)

    return {
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


def test_should_retry():
    err_good = ClientError({
        'Error': {
            'Code': 'ProvisionedThroughputExceededException',
            'Message': 'oops'
        }
    },'testing')

    err_bad = ClientError({
        'Error': {
            'Code': 'Bangarang!',
            'Message': 'oops'
        }
    },'testing')

    assert should_retry(err_good) == True
    assert should_retry(err_bad) == False


def test_request_reindex(reindex_shard_tracker_table):
    table = reindex_shard_tracker_table

    os.environ = {
        "TABLE_NAME": table.table_name
    }

    shard_id = "shard_id"
    another_shard_id = "an_unrelated_shard"
    current_version = 1
    desired_version = 2

    item1 = {
        'shardId': shard_id,
        'currentVersion': current_version,
        'desiredVersion': desired_version,
        'version': 0,
    }

    item2 = {
        'shardId': another_shard_id,
        'currentVersion': current_version,
        'desiredVersion': desired_version,
        'version': 0,
    }

    event1 = _create_event(
        shard_id,
        current_version,
        desired_version
    )

    event2 = _create_event(
        another_shard_id,
        current_version,
        desired_version
    )

    table.put_item(Item=item1)
    table.put_item(Item=item2)

    _run(table, event1)
    _run(table, event2)

    response_item = table.get_item(Key={'shardId': shard_id})

    print(response_item)

    assert response_item['Item']['shardId'] == shard_id
    assert response_item['Item']['currentVersion'] == desired_version
    assert response_item['Item']['desiredVersion'] == desired_version
    assert response_item['Item']['version'] == 1


def test_request_reindex_does_not_revert_current_version_update(reindex_shard_tracker_table):
    table = reindex_shard_tracker_table

    os.environ = {
        "TABLE_NAME": table.table_name
    }

    shard_id = "shard_id"
    current_version = 1
    desired_version = 2

    updated_version = 4

    event1 = _create_event(
        shard_id,
        current_version,
        desired_version
    )

    item = {
        'shardId': shard_id,
        'currentVersion': updated_version,
        'desiredVersion': updated_version,
        'version': 0,
    }

    table.put_item(Item=item)

    _run(table, event1)

    response_item = table.get_item(Key={'shardId': shard_id})

    assert response_item['Item']['shardId'] == shard_id
    assert response_item['Item']['currentVersion'] == updated_version
    assert response_item['Item']['desiredVersion'] == updated_version
    assert response_item['Item']['version'] == 0


def test_request_reindex_throws_conditional_update_exception(reindex_shard_tracker_table):
    table = reindex_shard_tracker_table

    os.environ = {
        "TABLE_NAME": table.table_name
    }

    shard_id = "shard_id"

    table.put_item(
        Item={
            'shardId': shard_id,
            'currentVersion': 1,
            'desiredVersion': 3,
        }
    )

    item = _process_reindex_tracker_update_job(table, {
        "shardId": shard_id,
        "completedReindexVersion": 3
    })

    table.put_item(
        Item={
            'shardId': shard_id,
            'currentVersion': 2,
            'desiredVersion': 2,
            'version': 2,
        }
    )

    with pytest.raises(ClientError) as e:
        _update_versioned_item(table, item)
        assert e.value.response['Error']['Code'] == 'ConditionalCheckFailedException'
#
