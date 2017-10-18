#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Run an

This is used to run one off ECS tasks.

The script is triggered by notifications to an SNS topic, in which the
message should be a JSON string that includes "cluster", "task_defintiion",
"name" and "desired_count" as attributes.
"""

import boto3

from wellcome_lambda_utils.ecs_utils import run_task
from wellcome_lambda_utils.sns_utils import extract_json_message


def main(event, _):
    print(f'event = {event!r}')

    message_data = extract_json_message(event)

    response = run_task(
        ecs_client=boto3.client('ecs'),
        cluster_name=message_data['cluster_name'],
        container_name=message_data['container_name'],
        task_definition=message_data['task_definition'],
        command=message_data['command'],
        started_by=message_data['started_by']
    )

    print(f'response = {response!r}')
    assert len(response['failures']) == 0
