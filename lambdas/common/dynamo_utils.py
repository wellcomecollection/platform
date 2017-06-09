# -*- encoding: utf-8 -*-

import boto3


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
            'WriteCapacityUnits':desired_capacity
        },
        GlobalSecondaryIndexUpdates=gsi_updates
    )
    print(f'DynamoDB response: {resp!r}')
    assert resp['ResponseMetadata']['HTTPStatusCode'] == 200
