import os

import boto3
from moto import mock_sns, mock_dynamodb2

import sns_to_dynamo


@mock_sns
@mock_dynamodb2
def test_should_insert_the_json_into_dynamo():
    sns_client = boto3.client('sns')
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

    create_topic_response = sns_client.create_topic(Name="test_miro_catalogue_api_topic")
    topic_arn = create_topic_response['TopicArn']

    miro_id = "A0000002"
    image_json = f"""{{
        "image_no_calc": "{miro_id}",
        "image_int_default":null,
        "image_artwork_date_from":"01/02/2000",
        "image_artwork_date_to":"13/12/2000",
        "image_barcode":"10000000",
        "image_creator":["Caspar Bauhin"]
    }}"""

    sns_client.publish(
        TopicArn=topic_arn,
        Message=image_json)

    os.environ = {
        "SNS_TOPIC_ARN": topic_arn,
        "TABLE_NAME": table_name
    }

    sns_to_dynamo.main()

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
