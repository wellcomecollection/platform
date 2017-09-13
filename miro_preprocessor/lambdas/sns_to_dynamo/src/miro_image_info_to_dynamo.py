import json
import os
import boto3


def main(event, _):
    print(f'Received event:\n{event}')

    client = boto3.client('dynamodb')
    table_name = os.environ["TABLE_NAME"]
    image_info = json.loads(event['Records'][0]['Sns']['Message'])
    image_data = image_info['image_data']
    collection = image_info['collection']

    print('Pushing image with ID %s' % (image_data['image_no_calc']))
    client.put_item(
        TableName=table_name,
        Item={
            'MiroID': {
                'S': image_data['image_no_calc']
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
