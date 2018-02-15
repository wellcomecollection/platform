# -*- encoding: utf-8 -*-

import os

import boto3
from botocore.exceptions import ClientError
from wellcome_aws_utils import sns_utils


def main(event, _ctxt=None, dynamodb_client=None):
    print(f'event = {event!r}')
    dynamodb_client = dynamodb_client or boto3.client('dynamodb')

    table_name = os.environ['TABLE_NAME']

    for record in sns_utils.extract_sns_messages_from_lambda_event(event):
        row = record.message
        try:
            dynamodb_client.update_item(
                TableName=table_name,
                Key={'shardId': {'S': row['shardId']}},
                UpdateExpression='SET currentVersion=:completedReindexVersion',
                ConditionExpression='currentVersion < :completedReindexVersion',
                ExpressionAttributeValues={
                    ':completedReindexVersion': {'N': str(row['completedReindexVersion'])},
                }
            )
        except ClientError as err:
            if err.response['Error']['Code'] == 'ConditionalCheckFailedException':
                print(f'{row["shardId"]} already has a newer currentVersion')
            else:
                raise
