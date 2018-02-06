# -*- encoding: utf-8 -*-
"""

"""

import os
from collections import namedtuple

from boto3.dynamodb.conditions import Attr
from botocore.exceptions import ClientError

import boto3

from wellcome_aws_utils import sns_utils


def _update_versioned_item(table, dynamo_item, desired_item):
    current_dynamo_conditional_update_version = dynamo_item['version']
    desired_dynamo_conditional_update_version = dynamo_item['version'] + 1

    new_dynamo_item = {**dynamo_item, **desired_item}
    new_dynamo_item['version'] = desired_dynamo_conditional_update_version

    print(f'Attempting conditional update to {new_dynamo_item}')

    table.put_item(
        Item=new_dynamo_item,
        ConditionExpression=Attr('version').eq(
            current_dynamo_conditional_update_version
        )
    )


def _process_reindex_tracker_update_job(table, message):
    shard_id = message['shardId']
    completed_reindex_version = message['completedReindexVersion']

    dynamodb_response = table.get_item(Key={'shardId': shard_id})

    print(dynamodb_response)

    dynamo_item = dynamodb_response['Item']

    print(f'Retrieved {dynamo_item}')

    dynamo_current_version = dynamo_item['currentVersion']

    if(dynamo_current_version >= completed_reindex_version):
        print(f'Update for {shard_id} discarded as current version advanced.')
        return

    desired_item = {
        "shardId": shard_id,
        "desiredVersion": completed_reindex_version,
        "currentVersion": completed_reindex_version,
    }

    return {
        "dynamo_item": dynamo_item,
        "desired_item": desired_item
    }

def _run(table, event):
    for record in sns_utils.extract_sns_messages_from_lambda_event(event):
        job = _process_reindex_tracker_update_job(table, record.message)

    if job is not None:
        _update_versioned_item(table, job['dynamo_item'], job['desired_item'])

def main(event, _):
    print(f'event = {event!r}')

    table_name = os.environ["TABLE_NAME"]
    dynamodb = boto3.resource('dynamodb')
    table = dynamodb.Table(table_name)

    _run(table, event)
