#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Change the read and write capacity of a DynamoDB Table and any global secondary index

The script is triggered by notifications to an SNS topic, in which the
message should be a JSON string that includes "dynamo_table_name" and "desired_capacity"
"""

import json

import boto3


def change_dynamo_capacity(table_name, desired_capacity):
    client = boto3.client('dynamodb')
    response = client.describe_table(TableName=table_name)
    gsi = [x['IndexName'] for x in response['Table']['GlobalSecondaryIndexes']]
    gsi_updates = [
        {'Update': {'IndexName': x, 'ProvisionedThroughput': {'ReadCapacityUnits': desired_capacity, 'WriteCapacityUnits': desired_capacity} }}
        for x in gsi
    ]

    resp = client.update_table(TableName=table_name, GlobalSecondaryIndexUpdates=gsi_updates, ProvisionedThroughput={'ReadCapacityUnits':desired_capacity, 'WriteCapacityUnits':desired_capacity} )
    print(f'DynamoDB response: {resp!r}')
    assert resp['ResponseMetadata']['HTTPStatusCode'] == 200


def main(event, _):
    print(f'Received event: {event!r}')
    message = event['Records'][0]['Sns']['Message']
    message_data = json.loads(message)

    change_dynamo_capacity(
        table_name=message_data['dynamo_table_name'],
        desired_capacity=message_data['desired_capacity']
    )
