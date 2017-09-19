import json
import os

import boto3
from moto import mock_dynamodb2
import pytest

import miro_image_to_dynamo


def pytest_runtest_setup(item):
    set_region()


@pytest.fixture()
def set_region():
    # Without this, boto3 is complaining about not having a region defined
    # in tests (despite one being set in the Travis env variables and passed
    # into the image).
    # TODO: Investigate this properly.
    boto3.setup_default_session(region_name='eu-west-1')


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


def test_should_insert_the_json_into_dynamo(miro_table):
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

    miro_image_to_dynamo.main(event, None)

    dynamodb_response = table.get_item(Key={'MiroID': miro_id, 'MiroCollection': collection})

    assert json.loads(dynamodb_response['Item']['data']) == image_data
    assert dynamodb_response['Item']['MiroCollection'] == collection
    assert dynamodb_response['Item']['ReindexShard'] == "default"
    assert dynamodb_response['Item']['ReindexVersion'] == 1
