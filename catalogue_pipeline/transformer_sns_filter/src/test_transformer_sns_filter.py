import json
import os

import boto3

import transformer_sns_filter


def test_transformer_sns_filter_does_not_send_null(sns_sqs):
    sqs_client = boto3.client('sqs')
    topic_arn, queue_url = sns_sqs
    os.environ['TOPIC_ARN'] = topic_arn

    dynamo_event = json.dumps({
        "event_type": "REMOVE",
        "new_image": None,
        "old_image": {
            "MiroID": "V0000001",
            "MiroCollection": "Images-V"
        }
    })

    event = {
        "Records": [
            {
                "EventSource": 'aws:sns',
                "Sns": {
                    "Message": dynamo_event,
                    "Subject": "default-subject"
                }

            }
        ]
    }

    transformer_sns_filter.main(event, None)

    messages = sqs_client.receive_message(
        QueueUrl=queue_url,
        MaxNumberOfMessages=1
    )

    assert 'Messages' not in messages