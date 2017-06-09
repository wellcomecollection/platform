# -*- encoding: utf-8 -*-

import pprint{'MessageId': '648b4d2b-1dd1-580c-bdca-1c9f546e2a29',
 'ResponseMetadata': {'HTTPHeaders': {'content-length': '294',
                                      'content-type': 'text/xml',
                                      'date': 'Fri, 09 Jun 2017 07:00:14 GMT',
                                      'x-amzn-requestid': '33719136-0980-51f3-81a3-c082403ff3d6'},
                      'HTTPStatusCode': 200,
                      'RequestId': '33719136-0980-51f3-81a3-c082403ff3d6',
                      'RetryAttempts': 0}}

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
    print(f'DynamoDB response:\n{pprint.pformat(resp)}')
    assert resp['ResponseMetadata']['HTTPStatusCode'] == 200
