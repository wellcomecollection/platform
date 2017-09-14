import json
import os

import boto3
from moto import mock_dynamodb2, mock_s3
import pytest

import miro_image_info_to_dynamo


@pytest.fixture
def miro_table():
    mock_dynamodb2().start()
    dynamodb = boto3.resource('dynamodb')

    table_name = "TestMiroData"

    table = dynamodb.create_table(
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
    yield table
    mock_dynamodb2().stop()


@pytest.fixture
def s3_miro_data_bucket():
    mock_s3().start()
    s3_client = boto3.client("s3")
    bucket_name = "test-miro-images-sync"
    s3_client.create_bucket(Bucket=bucket_name, ACL="private")
    yield bucket_name
    mock_s3().stop()


def test_should_insert_the_json_into_dynamo(miro_table, s3_miro_data_bucket):
    s3_client = boto3.client("s3")
    bucket_name = s3_miro_data_bucket
    s3_client.put_object(
        Bucket=bucket_name,
        ACL='private',
        Body=b'baba',Key="fullsize/A0000000/A0000002.jpg")
    table = miro_table

    miro_id = "A0000002"

    collection = "Images-C"
    image_data = {
        'image_no_calc': miro_id,
        'image_int_default': None,
        'image_artwork_date_from': "01/02/2000",
        'image_artwork_date_to': "13/12/2000",
        'image_barcode': "10000000",
        'image_creator': ["Caspar Bauhin"]
    }

    image_json = json.dumps({
        'collection': collection,
        'image_data': image_data
    })

    os.environ = {
        "TABLE_NAME": table.table_name,
        "MIRO_S3_BUCKET": bucket_name
    }

    event = {
        'Records': [{
            'EventSource': 'aws:sns',
            'EventVersion': '1.0',
            'EventSubscriptionArn':
                'arn:aws:sns:region:account_id:sns:stuff',
            'Sns': {
                'Type': 'Notification',
                'MessageId': 'b20eb72b-ffc7-5d09-9636-e6f65d67d10f',
                'TopicArn':
                    'arn:aws:sns:region:account_id:sns',
                'Subject': None,
                'Message': image_json,
                'Timestamp': '2017-07-10T15:42:24.307Z',
                'SignatureVersion': '1',
                'Signature': 'signature',
                'SigningCertUrl': 'https://certificate.pem',
                'UnsubscribeUrl': 'https://unsubscribe-url',
                'MessageAttributes': {}}
        }]
    }

    miro_image_info_to_dynamo.main(event, None)

    dynamodb_response = table.get_item(Key={'MiroID': miro_id, 'MiroCollection': collection})

    assert json.loads(dynamodb_response['Item']['data']) == image_data
    assert dynamodb_response['Item']['MiroCollection'] == collection
    assert dynamodb_response['Item']['ReindexShard'] == "default"
    assert dynamodb_response['Item']['ReindexVersion'] == 1


def test_should_not_insert_into_dynamodb_if_image_does_not_exist_in_s3(miro_table, s3_miro_data_bucket):
    table = miro_table
    bucket_name = s3_miro_data_bucket

    miro_id = "A0000002"

    collection = "Images-C"
    image_data = {
        'image_no_calc': miro_id,
        'image_int_default': None,
        'image_artwork_date_from': "01/02/2000",
        'image_artwork_date_to': "13/12/2000",
        'image_barcode': "10000000",
        'image_creator': ["Caspar Bauhin"]
    }

    image_json = json.dumps({
        'collection': collection,
        'image_data': image_data
    })
    os.environ = {
        "TABLE_NAME": table.table_name,
        "MIRO_S3_BUCKET": bucket_name
    }

    event = {
        'Records': [{
            'EventSource': 'aws:sns',
            'EventVersion': '1.0',
            'EventSubscriptionArn':
                'arn:aws:sns:region:account_id:sns:stuff',
            'Sns': {
                'Type': 'Notification',
                'MessageId': 'b20eb72b-ffc7-5d09-9636-e6f65d67d10f',
                'TopicArn':
                    'arn:aws:sns:region:account_id:sns',
                'Subject': None,
                'Message': image_json,
                'Timestamp': '2017-07-10T15:42:24.307Z',
                'SignatureVersion': '1',
                'Signature': 'signature',
                'SigningCertUrl': 'https://certificate.pem',
                'UnsubscribeUrl': 'https://unsubscribe-url',
                'MessageAttributes': {}}
        }]
    }

    miro_image_info_to_dynamo.main(event, None)

    dynamodb_response = table.get_item(Key={'MiroID': miro_id, 'MiroCollection': collection})

    print(dynamodb_response)

    assert 'Item' not in dynamodb_response.keys()
