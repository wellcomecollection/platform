import json
import os

import boto3


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
                'S': collection
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
    dynamodb_client = boto3.client('dynamodb')

    image_info = json.loads(event['Records'][0]['Sns']['Message'])
    image_data = image_info['image_data']
    collection = image_info['collection']
    miro_id = image_data['image_no_calc']

    put_into_dynamodb(dynamodb_client, miro_id, collection, image_data)
