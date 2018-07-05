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

from wellcome_aws_utils.lambda_utils import log_on_error

CONFIG_ENV_KEY = 'HEARTBEAT_CONFIG'


class HeartbeatConfig:
    def __init__(self, table_name, key, update_expression, expression_attribute_values):
        self.table_name = table_name
        self.key = key
        self.update_expression = update_expression
        self.expression_attribute_values = expression_attribute_values

def decode_config(dct):
    if '__heartbeat__' in dct:
        config_keys = ['__heartbeat__', 'table_name', 'key', 'update_expression', 'expression_attribute_values']
        if all(elem in dct.keys() for elem in config_keys):
            return HeartbeatConfig(table_name=dct['table_name'],
                                   key=dct['key'],
                                   update_expression=dct['update_expression'],
                                   expression_attribute_values=dct['expression_attribute_values'])
        else:
            raise RuntimeError("Incomplete HeartbeatConfig {}".format(dct))
    else:
        return dct


@log_on_error
def main(event, context, dynamodb_client=None):
    try:
        config = json.loads(os.environ[CONFIG_ENV_KEY], object_hook=decode_config)
    except KeyError:
        raise RuntimeError("{} not set".format(CONFIG_ENV_KEY))
    except json.JSONDecodeError:
        raise RuntimeError("config '{}' could not be parsed".format(os.environ[CONFIG_ENV_KEY]))

    dynamodb_client = dynamodb_client or boto3.client('dynamodb')
    for heartbeat in config:
        response = dynamodb_client.update_item(TableName=heartbeat.table_name,
                                               Key=heartbeat.key,
                                               UpdateExpression=heartbeat.update_expression,
                                               ExpressionAttributeValues=heartbeat.expression_attribute_values)
        print("Heartbeat, update_item on :{}, returned-status:{}".format(heartbeat.table_name, response['ResponseMetadata']['HTTPStatusCode']))
