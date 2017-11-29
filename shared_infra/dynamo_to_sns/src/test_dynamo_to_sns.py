import json
import os

import boto3

import dynamo_to_sns


def test_dynamo_to_sns_fails_gracefully_on_remove_event(sns_sqs):
    sqs_client = boto3.client('sqs')
    topic_arn, queue_url = sns_sqs
    stream_arn = 'arn:aws:dynamodb:eu-west-1:123456789012:table/table-stream'

    os.environ = {
        'TOPIC_ARN': topic_arn
    }

    old_image = {
        'MiroID': {'S': 'V0000001'},
        'MiroCollection': {'S': 'Images-V'},
    }

    expected_image = {
        "event_type": "REMOVE",
        "new_image": None,
        "old_image": {
            "MiroID": "V0000001",
            "MiroCollection": "Images-V"
        }
    }

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
                'OldImage': old_image,
                'SequenceNumber': '545308300000000005226392296', 'SizeBytes': 36,
                'StreamViewType': 'OLD_IMAGE'
            },
            'eventSourceARN': stream_arn
        }]
    }

    dynamo_to_sns.main(remove_event, None)

    messages = sqs_client.receive_message(
        QueueUrl=queue_url,
        MaxNumberOfMessages=1
    )

    assert len(messages['Messages']) == 1
    message_body = messages['Messages'][0]['Body']
    inner_message = json.loads(message_body)['Message']
    assert json.loads(json.loads(inner_message)['default']) == expected_image


def test_dynamo_to_sns(sns_sqs):
    sqs_client = boto3.client('sqs')
    topic_arn, queue_url = sns_sqs
    stream_arn = 'arn:aws:dynamodb:eu-west-1:123456789012:table/table-stream'

    new_image = {
        'ReindexVersion': {'N': '0'},
        'ReindexShard': {'S': 'default'},
        'data': {'S': 'test-json-data'},
        'MiroID': {'S': 'V0010033'},
        'MiroCollection': {'S': 'Images-V'}
    }

    expected_image = {
        "event_type": "MODIFY",
        "old_image": None,
        "new_image": {
            "ReindexVersion": 0,
            "ReindexShard": "default",
            "data": "test-json-data",
            "MiroID": "V0010033",
            "MiroCollection": "Images-V"
        }
    }

    event = {
        'Records': [
            {'eventID': '81659528846ddb9826c612c16043c2ea',
             'eventName': 'MODIFY',
             'eventVersion': '1.1',
             'eventSource': 'aws:dynamodb',
             'awsRegion': 'eu-west-1',
             'dynamodb': {
                 'ApproximateCreationDateTime': 1499243940.0,
                 'Keys': {
                     'MiroID': {'S': 'V0010033'},
                     'MiroCollection': {'S': 'Images-V'}
                 },
                 'NewImage': new_image,
                 'SequenceNumber': '167031600000000009949839133',
                 'SizeBytes': 6422,
                 'StreamViewType': 'NEW_IMAGE'
             },
             'eventSourceARN': stream_arn
             }
        ]
    }

    os.environ = {
        'TOPIC_ARN': topic_arn
    }

    dynamo_to_sns.main(event, None)

    messages = sqs_client.receive_message(
        QueueUrl=queue_url,
        MaxNumberOfMessages=1
    )
    assert len(messages['Messages']) == 1
    message_body = messages['Messages'][0]['Body']
    inner_message = json.loads(message_body)['Message']
    assert json.loads(inner_message)['default'] == json.dumps(expected_image)
