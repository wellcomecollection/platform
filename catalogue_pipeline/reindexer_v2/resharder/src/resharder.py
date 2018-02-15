# -*- encoding: utf-8

import os

import boto3
from wellcome_aws_utils.dynamo_event import DynamoEvent


def main(event, _ctxt=None, s3_client=None, dynamodb_client=None):
    print(f'event={event!r}')

    dynamodb_client = dynamodb_client or boto3.client('dynamodb')
    s3_client = s3_client or boto3.client('s3')

    table_name = os.environ['TABLE_NAME']
    bucket_name = os.environ['S3_BUCKET']

    for record in event['Records']:
        dynamo_event = DynamoEvent(record)
        row = dynamo_event.new_image(deserialize_values=True)

        if not row:
            print("no NewImage key in dynamo update event, skipping")
            continue

        from pprint import pprint
        pprint(row)

        if row.get('resharded'):
            print(f'{row["sourceId"]} has already been resharded')
            continue

        old_key = row['s3key']
        print(old_key)

        shard = row['sourceId'][::-1][:2]
        new_key = f'{row["sourceName"]}/{row["sourceId"]}/{row["sourceId"]}/{shard}/{os.path.basename(old_key)}'

        s3_client.copy_object(
            Bucket=bucket_name,
            Key=new_key,
            CopySource={
                'Bucket': bucket_name,
                'Key': old_key,
            }
        )

        version = int(row['version'])

        dynamodb_client.update_item(
            TableName=table_name,
            Key={'id': {'S': row['sourceId']}},
            UpdateExpression='SET version = :newVersion, resharded = :true, s3key = :newKey',
            ConditionExpression='version < :newVersion',
            ExpressionAttributeValues={
                ':newVersion': {'N': str(version + 1)},
                ':true': {'BOOL': True},
                ':newKey': {'S': new_key},
            }
        )
