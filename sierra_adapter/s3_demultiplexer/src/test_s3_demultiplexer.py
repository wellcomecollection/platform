# -*- encoding: utf-8 -*-

import json
import os

import boto3
from moto import mock_s3

from s3_demultiplexer import main


@mock_s3
def test_end_to_end_demultiplexer(sns_sqs):
    client = boto3.client("s3")

    records = [
        {'colour': 'red', 'letter': 'R'},
        {'colour': 'green', 'letter': 'G'},
        {'colour': 'blue', 'letter': 'B'},
    ]

    client.create_bucket(Bucket='bukkit')
    client.put_object(
        Bucket='bukkit',
        Key='test0001.json',
        Body=json.dumps(records)
    )

    s3_event = {
        "Records": [
            {
                "eventTime": "1970-01-01T00:00:00.000Z",
                "eventName": "event-type",
                "s3": {
                    "bucket": {
                        "name": "bukkit"
                    },
                    "object": {
                        "key": "test0001.json",
                        "size": len(json.dumps(records)),
                        "versionId": "v2"
                    }
                }
            }
        ]
    }

    sqs_client = boto3.client('sqs')
    topic_arn, queue_url = sns_sqs
    os.environ["TOPIC_ARN"] = topic_arn

    main(s3_event, None)

    messages = sqs_client.receive_message(
        QueueUrl=queue_url,
        MaxNumberOfMessages=len(records)
    )

    bodies = [m['Body'] for m in messages['Messages']]
    inner_messages = [json.loads(b)['Message'] for b in bodies]
    actual_messages = [
        json.loads(json.loads(inner_msg)['default'])
        for inner_msg in inner_messages
    ]

    assert actual_messages == records
