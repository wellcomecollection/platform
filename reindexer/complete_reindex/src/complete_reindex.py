# -*- encoding: utf-8 -*-

import os

from boto3.dynamodb.conditions import Attr
import boto3

from wellcome_aws_utils import sns_utils


def _update_versioned_item(table, item):
    print(f'Attempting conditional update to {item}')

    table.put_item(
        Item=item,
        ConditionExpression=Attr('version').not_exists() | Attr('version').lt(
            item['version']
        )
    )

    print(f'Successful update to {item}')


def _process_reindex_tracker_update_job(table, message):
    shard_id = message['shardId']
    completed_reindex_version = message['completedReindexVersion']

    print(f'Working: {shard_id} to {completed_reindex_version}')

    dynamodb_response = table.get_item(Key={'shardId': shard_id})
    dynamo_item = dynamodb_response['Item']

    print(f'Retrieved {dynamo_item}')

    dynamo_current_version = dynamo_item['currentVersion']

    if dynamo_current_version >= completed_reindex_version:
        print(f'Update for {shard_id} discarded.')
        return

    return {
        "shardId": shard_id,
        "desiredVersion": completed_reindex_version,
        "currentVersion": completed_reindex_version,
        "version": dynamo_item.get('version', 1) + 1
    }


def _run(table, event):
    for record in sns_utils.extract_sns_messages_from_lambda_event(event):
        item = _process_reindex_tracker_update_job(
            table,
            record.message
        )

        if item is not None:
            _update_versioned_item(table, item)
        else:
            print(f'No work to do for {record.message}')


def main(event, _):
    print(f'event = {event!r}')

    table_name = os.environ["TABLE_NAME"]
    dynamodb = boto3.resource('dynamodb')
    table = dynamodb.Table(table_name)

    _run(table, event)

    print(f'Done with {event!r}!')
