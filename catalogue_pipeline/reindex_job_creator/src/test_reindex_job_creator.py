# -*- encoding: utf-8 -*-

import boto3

from reindex_job_creator import main


def _wrap(image):
    """Given an image from a DynamoDB table, wrap it in an SNS event."""
    return {
        'Records': [
            {
                'eventID': '81659528846ddb9826c612c16043c2ea',
                'eventName': 'MODIFY',
                'eventVersion': '1.1',
                'eventSource': 'aws:dynamodb',
                'awsRegion': 'eu-west-1',
                'dynamodb': {
                    'NewImage': image,
                },
                'eventSourceARN': 'foo'
            }
        ]
    }


def test_higher_requested_version_triggers_job(queue_url):
    """
    If the DynamoDB table is updated so that desiredVersion > currentVersion,
    a job is sent to SQS.
    """
    image = {
        'shardId': 'example/1',
        'currentVersion': 2,
        'desiredVersion': 3,
    }

    main(event=_wrap(image), _ctxt=None)

    sqs_client = boto3.client('sqs')
    messages = sqs_client.receive_message(
        QueueUrl=queue_url,
        MaxNumberOfMessages=1
    )
    message_body = messages['Messages'][0]['Body']
    inner_message = json.loads(message_body)['Message']
    parsed_message = json.loads(json.loads(inner_message)['default'])

    assert parsed_message == {
        'shardId': 'example/1',
        'desiredVersion': 3,
    }
