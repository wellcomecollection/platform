# -*- encoding: utf-8 -*-
"""
Update a DynamoDB item, run in a scheduled lambda to make DynamoDB scale down when there would
otherwise be zero throughput.

HEARTBEAT_CONFIG: configuration
[
        {
            '__heartbeat__': True,
            'table_name': 'Table1',
            'key': {'id': {'S': 'heartbeat-dummy-record-id-1'}},
            'update_expression': "SET indexedId = :v",
            'expression_attribute_values': {':v': {'S': 'value-1'}}
        }
]
"""

import boto3
import os
import json
from botocore.exceptions import ClientError

from wellcome_aws_utils.lambda_utils import log_on_error

DEFAULT_HEARTBEAT_FIELD = "heartbeatId"

CONFIG_ENV_KEY = 'HEARTBEAT_CONFIG'

class HeartbeatConfig:
    def __init__(self, table_name, key, index):
        self.table_name = table_name
        self.key = key
        self.index = index


def decode_config(dct):
    if '__heartbeat__' in dct:
        config_keys = ['__heartbeat__', 'table_name', 'key']
        if all(elem in dct.keys() for elem in config_keys):
            return HeartbeatConfig(table_name=dct['table_name'],
                                   key=dct['key'],
                                   index=dct.get('index'))
        else:
            raise RuntimeError("Incomplete HeartbeatConfig {}".format(dct))
    else:
        return dct


@log_on_error
def main(event, context, dynamodb_client=None):
    try:
        heartbeat_config = json.loads(os.environ[CONFIG_ENV_KEY], object_hook=decode_config)
    except KeyError:
        raise RuntimeError("{} not set".format(CONFIG_ENV_KEY))
    except json.JSONDecodeError:
        raise RuntimeError("config '{}' could not be parsed".format(os.environ[CONFIG_ENV_KEY]))

    dynamodb_client = dynamodb_client or boto3.client('dynamodb')
    for heartbeat in heartbeat_config:
        try:
            condition_expression = "NOT :v = :v"
            update_expression = "SET {} = :v".format(heartbeat.index if heartbeat.index else DEFAULT_HEARTBEAT_FIELD)
            expression_attribute_values = {':v': {'S': 'heartbeat'}}

            response = dynamodb_client.update_item(TableName=heartbeat.table_name,
                                                   Key=heartbeat.key,
                                                   ConditionExpression=condition_expression,
                                                   UpdateExpression=update_expression,
                                                   ExpressionAttributeValues=expression_attribute_values)
            print("Heartbeat, failed to return expected ConditionalCheckFailedException returned:{} on table:{}".format(
                response, heartbeat.table_name))
        except ClientError as e:
            if e.response.get('Error').get('Code') == 'ConditionalCheckFailedException':
                # The conditional check prevents anything being updated, and is expected, capacity is still consumed.
                print("Heartbeat, update_item on dynamoDB table:{}".format(heartbeat.table_name))
            else:
                print("Heartbeat, failed to return expected response:{}, on table:{}".format(
                    e.response, heartbeat.table_name))
