# -*- encoding: utf-8 -*-

import boto3
from boto3.dynamodb.types import TypeDeserializer
import pprint


class DynamoEvent:
    def __init__(self, event):
        self.event = event

    @property
    def new_image(self):
        return self.event['Records'][0]['dynamodb']['NewImage']

    @property
    def source_arn(self):
        return self.event['Records'][0]['eventSourceARN']

    @property
    def simplified_new_image(self):
        image = self.event['Records'][0]['dynamodb']['NewImage']

        td = TypeDeserializer()

        return {k: td.deserialize(v) for k, v in image.items()}


def change_dynamo_capacity(table_name, desired_capacity):
    """
    Given the name of a DynamoDB table and a desired capacity, update the
    read/write capacity of the table and every secondary index.
    """

    client = boto3.client('dynamodb')
    response = client.describe_table(TableName=table_name)

    gsi_names = [
        idx['IndexName'] for idx in response['Table']['GlobalSecondaryIndexes']
    ]

    gsi_updates = [
        {
            'Update': {
                'IndexName': index_name,
                'ProvisionedThroughput': {
                    'ReadCapacityUnits': desired_capacity,
                    'WriteCapacityUnits': desired_capacity
                }
            }
        }
        for index_name in gsi_names
    ]

    resp = client.update_table(
        TableName=table_name,
        ProvisionedThroughput={
            'ReadCapacityUnits': desired_capacity,
            'WriteCapacityUnits': desired_capacity
        },
        GlobalSecondaryIndexUpdates=gsi_updates
    )

    print(f'DynamoDB response:\n{pprint.pformat(resp)}')
    assert resp['ResponseMetadata']['HTTPStatusCode'] == 200
