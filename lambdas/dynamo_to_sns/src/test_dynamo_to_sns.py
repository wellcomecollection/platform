import json
import os

import boto3

import dynamo_to_sns


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
        'ReindexVersion': 0,
        'ReindexShard': 'default',
        'data': 'test-json-data',
        'MiroID': 'V0010033',
        'MiroCollection': 'Images-V'
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

    stream_arn_map = {
        stream_arn: topic_arn
    }
    os.environ = {
        "STREAM_TOPIC_MAP": json.dumps(stream_arn_map)
    }

    dynamo_to_sns.main(event, None)

    messages = sqs_client.receive_message(
        QueueUrl=queue_url,
        MaxNumberOfMessages=1
    )
    message_body = messages['Messages'][0]['Body']
    assert json.loads(message_body)['default'] == json.dumps(expected_image)
