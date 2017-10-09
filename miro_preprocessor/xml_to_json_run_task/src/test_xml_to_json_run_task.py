# -*- encoding: utf-8 -*-
import os

import boto3
import json

import xml_to_json_run_task


def s3_event():
    return {
        "Records": [
            {
                "eventTime": "1970-01-01T00:00:00.000Z",
                "eventName": "event-type",
                "s3": {
                    "bucket": {
                        "name": "bucket-name"
                    },
                    "object": {
                        "key": "object-key",
                        "size": 1234,
                        "versionId": "v2"
                    }
                }
            }
        ]
    }


def test_xml_to_json_run_task(sns_sqs):
    e = s3_event()

    sqs_client = boto3.client('sqs')
    topic_arn, queue_url = sns_sqs

    cluster_name = "cluster_name"
    container_name = "container_name"
    task_definition_arn = "task_definition_arn"

    os.environ["TOPIC_ARN"] = topic_arn
    os.environ["CLUSTER_NAME"] = cluster_name
    os.environ["CONTAINER_NAME"] = container_name
    os.environ["TASK_DEFINITION_ARN"] = task_definition_arn

    xml_to_json_run_task.main(e, {})

    messages = sqs_client.receive_message(
        QueueUrl=queue_url,
        MaxNumberOfMessages=1
    )

    message_body = messages['Messages'][0]['Body']
    inner_message = json.loads(message_body)['Message']

    actual_message = json.loads(
        json.loads(inner_message)['default']
    )

    expected_message = {
        "cluster_name": cluster_name,
        "container_name": container_name,
        "task_definition": task_definition_arn,
        "started_by": "xml_to_json_run_task",
        "command": [
            '--bucket=bucket-name',
            '--src=object-key',
            '--dst=object-key.txt'
        ]
    }

    assert actual_message == expected_message
