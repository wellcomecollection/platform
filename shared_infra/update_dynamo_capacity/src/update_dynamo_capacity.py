#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Change the read and write capacity of a DynamoDB Table and any global
secondary index.

The script is triggered by notifications to an SNS topic, in which the
message should be a JSON string that includes "dynamo_table_name"
and "desired_capacity".
"""

import json

import boto3

from wellcome_lambda_utils.dynamo_utils import change_dynamo_capacity


def update_dynamo_capacity(client, event):
    print(f'event = {event!r}')
    message = event['Records'][0]['Sns']['Message']
    message_data = json.loads(message)
    change_dynamo_capacity(
        client=client,
        table_name=message_data['dynamo_table_name'],
        desired_capacity=message_data['desired_capacity']
    )


def main(event, _):
    client = boto3.client('dynamodb')
    update_dynamo_capacity(client, event)
