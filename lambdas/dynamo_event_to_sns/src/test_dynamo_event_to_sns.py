import json
import os

import boto3

import dynamo_event_to_sns


modify_record = {
    'eventID': '81659528846ddb9826c612c16043c2ea',
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
        'NewImage': {
            'ReindexVersion': {'N': '0'},
            'ReindexShard': {'S': 'default'},
            'data': {'S': 'test-json-data-new'},
            'MiroID': {'S': 'V0010033'},
            'MiroCollection': {'S': 'Images-V'}
        },
        'OldImage': {
            'ReindexVersion': {'N': '0'},
            'ReindexShard': {'S': 'default'},
            'data': {'S': 'test-json-data-old'},
            'MiroID': {'S': 'V0010033'},
            'MiroCollection': {'S': 'Images-V'}
        },
        'SequenceNumber': '167031600000000009949839133',
        'SizeBytes': 6422,
        'StreamViewType': 'NEW_AND_OLD_IMAGES'
    },
    'eventSourceARN': 'arn:aws:dynamodb:eu-west-1:123456789012:table/table-stream'
}

insert_record = {
    'eventID': '81659528846ddb9826c612c16043c2ea',
    'eventName': 'INSERT',
    'eventVersion': '1.1',
    'eventSource': 'aws:dynamodb',
    'awsRegion': 'eu-west-1',
    'dynamodb': {
        'ApproximateCreationDateTime': 1499243940.0,
        'Keys': {
            'MiroID': {'S': 'V0010033'},
            'MiroCollection': {'S': 'Images-V'}
        },
        'NewImage': {
            'ReindexVersion': {'N': '0'},
            'ReindexShard': {'S': 'default'},
            'data': {'S': 'test-json-data-new'},
            'MiroID': {'S': 'V0010033'},
            'MiroCollection': {'S': 'Images-V'}
        },
        'SequenceNumber': '167031600000000009949839133',
        'SizeBytes': 6422,
        'StreamViewType': 'NEW_AND_OLD_IMAGES'
    },
    'eventSourceARN': 'arn:aws:dynamodb:eu-west-1:123456789012:table/table-stream'
}

remove_record = {
    'eventID': '81659528846ddb9826c612c16043c2ea',
    'eventName': 'REMOVE',
    'eventVersion': '1.1',
    'eventSource': 'aws:dynamodb',
    'awsRegion': 'eu-west-1',
    'dynamodb': {
        'ApproximateCreationDateTime': 1499243940.0,
        'Keys': {
            'MiroID': {'S': 'V0010033'},
            'MiroCollection': {'S': 'Images-V'}
        },
        'OldImage': {
            'ReindexVersion': {'N': '0'},
            'ReindexShard': {'S': 'default'},
            'data': {'S': 'test-json-data-old'},
            'MiroID': {'S': 'V0010033'},
            'MiroCollection': {'S': 'Images-V'}
        },
        'SequenceNumber': '167031600000000009949839133',
        'SizeBytes': 6422,
        'StreamViewType': 'NEW_AND_OLD_IMAGES'
    },
    'eventSourceARN': 'arn:aws:dynamodb:eu-west-1:123456789012:table/table-stream'
}

event = {
        'Records': [modify_record, insert_record, remove_record]
}


def _load_message(messages):
    message_body = messages['Messages'][0]['Body']
    return json.loads(message_body)['Message']


def test_dynamo_event_to_sns(dynamo_to_sns_event_sns_sqs):
    sqs_client = boto3.client('sqs')
    sns_sqs = dynamo_to_sns_event_sns_sqs

    os.environ = {
        "REMOVE_TOPIC_ARN": sns_sqs['remove']['topic'],
        "MODIFY_TOPIC_ARN": sns_sqs['modify']['topic'],
        "INSERT_TOPIC_ARN": sns_sqs['insert']['topic']
    }

    dynamo_event_to_sns.main(event, None)

    remove_message = sqs_client.receive_message(
        QueueUrl=sns_sqs['remove']['queue'],
        MaxNumberOfMessages=1
    )

    actual_remove_record = _load_message(remove_message.get("Messages"))
    assert actual_remove_record == remove_record

    modify_message = sqs_client.receive_message(
        QueueUrl=sns_sqs['modify']['queue'],
        MaxNumberOfMessages=1
    )

    actual_modify_record = _load_message(modify_message.get("Messages"))
    assert actual_modify_record == modify_record

    insert_message = sqs_client.receive_message(
        QueueUrl=sns_sqs['insert']['queue'],
        MaxNumberOfMessages=1
    )

    actual_insert_record = _load_message(insert_message.get("Messages"))
    assert actual_insert_record == modify_record
