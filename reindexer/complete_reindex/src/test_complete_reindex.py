# -*- encoding: utf-8 -*-

import json
import os
import time

from botocore.exceptions import ClientError
import pytest

from complete_reindex import main


shard_id = "shard_id"
current_version = 1
desired_version = 2

example_message = {
    "shardId": shard_id,
    "completedReindexVersion": desired_version
}

encoded_example_message = json.dumps(example_message)

def _event(shard_id, completed_version):
    message = {
        'shardId': shard_id,
        'completedReindexVersion': completed_version
    }

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
                    "Message": json.dumps(message),
                    "MessageAttributes": {},
                    "Type": "Notification",
                    "UnsubscribeUrl": "EXAMPLE",
                    "TopicArn": "topicarn",
                    "Subject": "TestInvoke"
                }
            }
        ]
    }


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


def test_applies_update(dynamodb_client, reindex_shard_tracker_table):
    dynamodb_client.put_item(
        TableName=reindex_shard_tracker_table,
        Item={
            'shardId': {'S': 'example/111'},
            'currentVersion': {'N': '1'},
            'desiredVersion': {'N': '3'},
        }
    )

    main(
        event=_event(shard_id='example/111', completed_version=3),
        dynamodb_client=dynamodb_client
    )

    resp = dynamodb_client.get_item(
        TableName=reindex_shard_tracker_table,
        Key={'shardId': {'S': 'example/111'}}
    )

    assert resp['Item']['shardId']['S'] == 'example/111'
    assert resp['Item']['currentVersion']['N'] == '3'
    assert resp['Item']['desiredVersion']['N'] == '3'


def test_does_not_rollback_current_version(dynamodb_client, reindex_shard_tracker_table):
    dynamodb_client.put_item(
        TableName=reindex_shard_tracker_table,
        Item={
            'shardId': {'S': 'example/222'},
            'currentVersion': {'N': '2'},
            'desiredVersion': {'N': '2'},
        }
    )

    main(
        event=_event(shard_id='example/222', completed_version=1),
        dynamodb_client=dynamodb_client
    )

    # Give the update time to apply, if it's going to.
    time.sleep(1)

    resp = dynamodb_client.get_item(
        TableName=reindex_shard_tracker_table,
        Key={'shardId': {'S': 'example/222'}}
    )

    assert resp['Item']['shardId']['S'] == 'example/222'
    assert resp['Item']['currentVersion']['N'] == '2'
    assert resp['Item']['desiredVersion']['N'] == '2'


def test_other_clienterror_is_raised(dynamodb_client, reindex_shard_tracker_table):
    dynamodb_client.put_item(
        TableName=reindex_shard_tracker_table,
        Item={
            'shardId': {'S': 'example/222'},
            'currentVersion': {'N': '2'},
            'desiredVersion': {'N': '2'},
        }
    )

    old_update = dynamodb_client.update_item

    def bad_update_items(**kwargs):
        kwargs['TableName'] = 'doesnotexist'
        old_update(**kwargs)

    dynamodb_client.update_item = bad_update_items

    with pytest.raises(ClientError):
        main(
            event=_event(shard_id='example/222', completed_version=3),
            dynamodb_client=dynamodb_client
        )
