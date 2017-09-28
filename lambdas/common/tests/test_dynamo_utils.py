from utils import dynamo_utils


event_source_arn = "arn:aws:dynamodb:us-east-1:123456789012:table/BarkTable/stream/2016-11-16T20:42:48.104"

dynamodb_record = {
    "ApproximateCreationDateTime": 1479499740,
    "Keys": {
        "Timestamp": {
            "S": "2016-11-18:12:09:36"
        },
        "Username": {
            "S": "John Doe"
        }
    },
    "NewImage": {
        "Timestamp": {
            "S": "2016-11-18:12:09:36"
        },
        "Message": {
            "S": "This is a bark from the Woofer social network"
        },
        "Username": {
            "S": "John Doe"
        }
    },
    "SequenceNumber": "13021600000000001596893679",
    "SizeBytes": 112,
    "StreamViewType": "NEW_IMAGE"
}

insert_record = {
    "eventID": "7de3041dd709b024af6f29e4fa13d34c",
    "eventName": "INSERT",
    "eventVersion": "1.1",
    "eventSource": "aws:dynamodb",
    "awsRegion": "us-west-2",
    "dynamodb": dynamodb_record,
    "eventSourceARN": event_source_arn
}

remove_record = {
    'eventID': '87cf2ca0f689908d573fb3698a487bb1',
    'eventName': 'REMOVE',
    'eventVersion': '1.1',
    'eventSource': 'aws:dynamodb',
    'awsRegion': 'eu-west-1',
    'dynamodb': {
        'ApproximateCreationDateTime': 1505815200.0,
        'Keys': {
            'MiroID': {
                'S': 'V0000001'
            },
            'MiroCollection': {
                'S': 'Images-V'
            }
        },
        'SequenceNumber': '545308300000000005226392296', 'SizeBytes': 36,
        'StreamViewType': 'NEW_IMAGE'
    },
    'eventSourceARN': event_source_arn
}


def _wrap(record):
    return {'Records': [record]}


def test_getting_new_image_where_not_available_returns_none():
    dynamo_image = dynamo_utils.DynamoImage(remove_record, event_source_arn)
    assert dynamo_image.simplified_new_image is None
    assert dynamo_image.new_image is None


def test_get_source_arn():
    dynamo_image = dynamo_utils.DynamoImage(insert_record, event_source_arn)
    assert dynamo_image.source_arn == event_source_arn


def test_getting_new_image_from_factory_where_not_available_returns_empty_list():
    dynamo_images = dynamo_utils.DynamoImageFactory.create(_wrap(remove_record))
    assert dynamo_images == []


def test_getting_new_image_from_factory():
    dynamo_images = dynamo_utils.DynamoImageFactory.create(_wrap(insert_record))

    actual_image = dynamo_images[0]
    expected_image = dynamo_utils.DynamoImage(
        dynamodb_record,
        event_source_arn
    )

    assert actual_image.simplified_new_image == expected_image.simplified_new_image
