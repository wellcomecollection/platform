# -*- encoding: utf-8 -*-

"""
Lambda to run the Miro XML to JSON task when receiving an S3 event.
"""

import os

import boto3

import s3_utils
import sns_utils


def command_for_xml_to_json_task(event, cluster_name, container_name, task_definition):
    bucket_name = event["bucket_name"]
    object_src = event["object_key"]
    object_dst = f'{object_src}.txt'

    cmd = [
        f'--bucket={bucket_name}',
        f'--src={object_src}',
        f'--src={object_dst}'
    ]

    return {
        "cluster_name": cluster_name,
        "container_name": container_name,
        "task_definition": task_definition,
        "started_by": "xml_to_json_run_task",
        "command": cmd,
    }


def post_to_sns(sns_client, topic_arn, cmd):
    return sns_utils.publish_sns_message(
        sns_client,
        topic_arn,
        cmd
    )


def main(event, _):
    print(f'event = {event!r}')

    topic_arn = os.environ["TOPIC_ARN"]
    cluster_name = os.environ["CLUSTER_NAME"]
    container_name = os.environ["CONTAINER_NAME"]
    task_definition = os.environ["TASK_DEFINITION_ARN"]

    sns_client = boto3.client('sns')

    s3_events = s3_utils.parse_s3_record(event=event)
    task_commands = [command_for_xml_to_json_task(
        event,
        cluster_name,
        container_name,
        task_definition
    ) for event in s3_events]

    [post_to_sns(sns_client, topic_arn, cmd) for cmd in task_commands]
