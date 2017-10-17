from src.wellcome_lambda_utils import dynamo_utils

event_source_arn = "arn:aws:dynamodb:us-east-1:123456789012:table/BarkTable/stream/2016-11-16T20:42:48.104"


def create_dynamodb_record(message):
    return {
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
                "S": message
            },
            "Username": {
                "S": "John Doe"
            }
        },
        "SequenceNumber": "13021600000000001596893679",
        "SizeBytes": 112,
        "StreamViewType": "NEW_IMAGE"
    }


def create_insert_record(message):
    return {
        "eventID": "7de3041dd709b024af6f29e4fa13d34c",
        "eventName": "INSERT",
        "eventVersion": "1.1",
        "eventSource": "aws:dynamodb",
        "awsRegion": "us-west-2",
        "dynamodb": create_dynamodb_record(message),
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


def _wrap(records):
    return {'Records': records}


def test_getting_new_image_where_not_available_returns_none():
    dynamo_image = dynamo_utils.DynamoImage(remove_record, event_source_arn)
    assert dynamo_image.simplified_new_image is None
    assert dynamo_image.new_image is None


def test_get_source_arn():
    dynamo_image = dynamo_utils.DynamoImage(create_insert_record("foo"), event_source_arn)
    assert dynamo_image.source_arn == event_source_arn


def test_getting_new_image_from_factory_where_not_available_returns_empty_list():
    dynamo_images = dynamo_utils.DynamoImageFactory.create(_wrap([remove_record]))
    assert dynamo_images == []


def test_getting_new_image_from_factory():
    expected_messages = [f"message: {i}" for i in range(1, 10)]

    records = [create_insert_record(message) for message in expected_messages]
    dynamo_images = dynamo_utils.DynamoImageFactory.create(_wrap(records))
    actual_simplified_images = [dynamo_image.simplified_new_image for dynamo_image in dynamo_images]

    actual_messages = [image["Message"] for image in actual_simplified_images]

    assert sorted(expected_messages) == sorted(actual_messages)
