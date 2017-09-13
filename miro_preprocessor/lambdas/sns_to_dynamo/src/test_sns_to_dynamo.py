import json
import os

import boto3
from moto import mock_dynamodb2

import sns_to_dynamo


@mock_dynamodb2
def test_should_insert_the_json_into_dynamo():
    dynamodb_client = boto3.client('dynamodb')

    table_name = "TestMiroData"
    dynamodb_client.create_table(
        TableName=table_name,
        KeySchema=[
            {
                'AttributeName': 'MiroID',
                'KeyType': 'HASH'
            },
            {
                'AttributeName': 'MiroCollection',
                'KeyType': 'RANGE'
            },
        ],
        AttributeDefinitions=[
            {
                'AttributeName': "MiroID",
                'AttributeType': "S"
            },

            {
                'AttributeName': "MiroCollection",
                'AttributeType': "S"
            },

            {
                'AttributeName': "ReindexShard",
                'AttributeType': "S"
            },

            {
                'AttributeName': "ReindexVersion",
                'AttributeType': "N"
            }
        ],
        GlobalSecondaryIndexes=[
            {
                'IndexName': "ReindexTracker",
                'KeySchema': [
                    {
                        'AttributeName': "ReindexShard",
                        'KeyType': 'HASH'
                    },
                    {
                        'AttributeName': "ReindexVersion",
                        'KeyType': 'RANGE'
                    }
                ],
                'Projection': {
                    'ProjectionType': 'ALL'
                },
                'ProvisionedThroughput': {
                    'ReadCapacityUnits': 1,
                    'WriteCapacityUnits': 1
                }
            }
        ],
        ProvisionedThroughput={
            'ReadCapacityUnits': 1,
            'WriteCapacityUnits': 1
        }
    )

    miro_id = "A0000002"
    image_json = f"""{{
        "image_no_calc": "{miro_id}",
        "image_int_default":null,
        "image_artwork_date_from":"01/02/2000",
        "image_artwork_date_to":"13/12/2000",
        "image_barcode":"10000000",
        "image_creator":["Caspar Bauhin"]
    }}"""

    os.environ = {
        "TABLE_NAME": table_name
    }

    event = {
        'Records': [{
            'EventSource': 'aws:sns',
            'EventVersion': '1.0',
            'EventSubscriptionArn':
                'arn:aws:sns:region:account_id:alb_server_error_alarm:stuff',
            'Sns': {
                'Type': 'Notification',
                'MessageId': 'b20eb72b-ffc7-5d09-9636-e6f65d67d10f',
                'TopicArn':
                    'arn:aws:sns:region:account_id:alb_server_error_alarm',
                'Subject':
                    'ALARM: "api-alb-target-500-errors" in EU - Ireland',
                'Message': json.dumps(image_json),
                'Timestamp': '2017-07-10T15:42:24.307Z',
                'SignatureVersion': '1',
                'Signature': 'signature',
                'SigningCertUrl': 'https://certificate.pem',
                'UnsubscribeUrl': 'https://unsubscribe-url',
                'MessageAttributes': {}}
        }]
    }

    sns_to_dynamo.main(event, None)

    dynamodb_response = dynamodb_client.get_item(
        TableName=table_name,
        Key={
            'MiroID': {
                'S': miro_id
            }
        })

    assert dynamodb_response['Item']['data'] == image_json
    assert dynamodb_response['Item']['MiroCollection'] == "bu"
    assert dynamodb_response['Item']['ReindexShard'] == "default"
    assert dynamodb_response['Item']['ReindexVersion'] == 0
