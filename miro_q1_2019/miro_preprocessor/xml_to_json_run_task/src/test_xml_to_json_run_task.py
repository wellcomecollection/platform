# -*- encoding: utf-8 -*-

import os

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


def test_xml_to_json_run_task(sns_client, topic_arn):

    cluster_name = "cluster_name"
    container_name = "container_name"
    task_definition_arn = "task_definition_arn"

    os.environ.update({
        'CLUSTER_NAME': cluster_name,
        'CONTAINER_NAME': container_name,
        'TASK_DEFINITION_ARN': task_definition_arn,
    })

    xml_to_json_run_task.main(event=s3_event(), sns_client=sns_client)

    messages = sns_client.list_messages()
    assert len(messages) == 1
    actual_message = messages[0][':message']

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
