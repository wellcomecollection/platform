#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
This is used to run one off ECS tasks.

The script is triggered by notifications to an SNS topic, in which the
message should be a JSON string that includes "cluster", "task_definition_arn",
"name" and "desired_count" as attributes.
"""

import collections
import os

import boto3
import daiquiri
import logging
import maya

from wellcome_lambda_utils.sns_utils import extract_json_message

daiquiri.setup(level=logging.INFO)
logger = daiquiri.getLogger(__name__)

RunEcsTaskResponse = collections.namedtuple(
    'RunEcsTaskResponse',
    'task_arn request created_at'
)

RunEcsTaskRequest = collections.namedtuple(
    'RunEcsTaskRequest',
    'task_definition_arn cluster_name container_name command started_by'
)


class MissingParameters(Exception):
    pass


class FailedRunningTask(Exception):
    pass


def _extract_run_ecs_task_response(response, request):
    def _build_run_ecs_task_response(task, request):
        created_at = maya.MayaDT.from_datetime(task['createdAt']).iso8601()

        return RunEcsTaskResponse(
            task_arn=task['taskArn'],
            request=request,
            created_at=created_at
        )

    return [
        _build_run_ecs_task_response(task, request) for task in response['tasks']
    ]


def _extract_run_ecs_task_request(event):
    message_data = extract_json_message(event)

    if 'cluster_name' not in message_data:
        raise MissingParameters('cluster_name param missing!')

    if 'task_definition_arn' not in message_data:
        raise MissingParameters('task_definition_arn param missing!')

    command = []
    if 'command' in message_data:
        command = message_data['command']

    container_name = 'app'
    if 'container_name' in message_data:
        container_name = message_data['container_name']

    started_by = ''
    if 'started_by' in message_data:
        started_by = message_data['started_by']

    return RunEcsTaskRequest(
        task_definition_arn=message_data['task_definition_arn'],
        cluster_name=message_data['cluster_name'],
        container_name=container_name,
        command=command,
        started_by=started_by
    )


def _run_task(ecs_client, run_ecs_task_request):
    task_definition = run_ecs_task_request.task_definition_arn.split('/')[-1]

    return ecs_client.run_task(
        cluster=run_ecs_task_request.cluster_name,
        taskDefinition=task_definition,
        overrides={
            'containerOverrides': [
                {
                    'name': run_ecs_task_request.container_name,
                    'command': run_ecs_task_request.command
                },
            ]
        },
        count=1,
        startedBy=run_ecs_task_request.started_by,
    )


def _report_failures(response):
    if len(response['failures']) != 0:
        for failure in response['failures']:
            logger.error(failure)

        raise FailedRunningTask()


def _put_run_ecs_task_request_in_dynamo(table, run_ecs_task_response):
    return table.put_item(
        Item={
            'task_arn': run_ecs_task_response.task_arn,
            'task_definition_arn': run_ecs_task_response.request.task_definition_arn,
            'cluster_name': run_ecs_task_response.request.cluster_name,
            'container_name': run_ecs_task_response.request.container_name,
            'command': run_ecs_task_response.request.command,
            'started_by': run_ecs_task_response.request.started_by
        }
    )


def _register_tasks(response, table, run_ecs_task_request):
    ecs_task_responses = _extract_run_ecs_task_response(
        response,
        run_ecs_task_request
    )

    responses = []
    for ecs_task_response in ecs_task_responses:
        response = _put_run_ecs_task_request_in_dynamo(
            table,
            ecs_task_response
        )

        responses.append(response)

    return responses


def main(event, _):
    print(f'event = {event!r}')

    table_name = os.environ["TABLE_NAME"]

    ecs_client = boto3.client('ecs')
    dynamodb = boto3.resource('dynamodb')
    table = dynamodb.Table(table_name)

    run_ecs_task_request = _extract_run_ecs_task_request(event)
    response = _run_task(ecs_client, run_ecs_task_request)

    _report_failures(response)
    _register_tasks(response, table, run_ecs_task_request)
