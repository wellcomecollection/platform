import json
import os
import re

import boto3
from botocore.exceptions import ClientError


def put_into_dynamodb(dynamodb_client, miro_id, collection, image_data):
    print(f"Image found for MiroId {miro_id}: sending to Dynamodb")
    table_name = os.environ["TABLE_NAME"]
    print('Pushing image with ID %s' % (miro_id))
    dynamodb_client.put_item(
        TableName=table_name,
        Item={
            'MiroID': {
                'S': miro_id
            },
            'MiroCollection': {
                'S': collection
            },
            'ReindexShard': {
                'S': 'default'
            },
            'ReindexVersion': {
                'N': str(1)
            },
            'data': {
                'S': json.dumps(image_data, separators=(',', ':'))
            }
        }
    )


def main(event, _):
    print(f'Received event:\n{event}')
    s3_client = boto3.client("s3")
    dynamodb_client = boto3.client('dynamodb')

    bucket_name = os.environ["MIRO_S3_BUCKET"]

    image_info = json.loads(event['Records'][0]['Sns']['Message'])
    image_data = image_info['image_data']
    collection = image_info['collection']
    miro_id = image_data['image_no_calc']
    result = re.match(r"(?P<shard>[A-Z]+[0-9]{4})", miro_id)
    key = f"fullsize/{result.group('shard')}000/{miro_id}.jpg"

    try:
        s3_client.head_object(Bucket=bucket_name, Key=key)
    except ClientError as client_error:
        if client_error.response['Error']['Code'] == '404':
            print(f"No image found for MiroId {miro_id}: skipping")
            pass
        else:
            raise
    else:
        put_into_dynamodb(dynamodb_client, miro_id, collection, image_data)
