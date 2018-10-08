# -*- encoding: utf-8 -*-
"""
Lambda to call the RunTask API for the Miro XML-to-JSON ECS task when a file in S3 is updated.
"""

import os

import boto3

from wellcome_lambda_utils import s3_utils
from wellcome_lambda_utils import sns_utils


def command_for_xml_to_json_task(event, cluster_name, container_name, task_definition):
    bucket_name = event["bucket_name"]
    object_src = event["object_key"]
    object_dst = f'{object_src}.txt'

    cmd = [
        f'--bucket={bucket_name}',
        f'--src={object_src}',
        f'--dst={object_dst}'
    ]

    return {
        "cluster_name": cluster_name,
        "container_name": container_name,
        "task_definition": task_definition,
        "started_by": "xml_to_json_run_task",
        "command": cmd,
    }


def main(event, _ctxt=None, sns_client=None):
    print(f'event = {event!r}')

    topic_arn = os.environ["TOPIC_ARN"]
    cluster_name = os.environ["CLUSTER_NAME"]
    container_name = os.environ["CONTAINER_NAME"]
    task_definition = os.environ["TASK_DEFINITION_ARN"]

    sns_client = sns_client or boto3.client('sns')

    s3_events = s3_utils.parse_s3_record(event=event)
    task_commands = [command_for_xml_to_json_task(
        event,
        cluster_name,
        container_name,
        task_definition
    ) for event in s3_events]

    for cmd in task_commands:
        sns_utils.publish_sns_message(
            sns_client,
            topic_arn,
            cmd,
            subject="xml_to_json_run_task"
        )
