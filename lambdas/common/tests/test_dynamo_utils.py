from utils import dynamo_utils

remove_event = {'Records': [
    {
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
        'eventSourceARN': 'arn:aws:dynamodb:eu-west-1:760097843905:table/MiroData/stream/2017-06-01T12:51:55.680'
    }]
}

def test_getting_new_image_where_not_available_returns_none():
    dynamo_event = dynamo_utils.DynamoEvent(remove_event)

    assert dynamo_event.simplified_new_image == None
    assert dynamo_event.new_image == None