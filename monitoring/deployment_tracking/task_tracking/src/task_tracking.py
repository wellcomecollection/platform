#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Tracking task status in ECS!

Because we run jobs in ECS and would like to know when they're done.
"""

import collections
import datetime
import os

import boto3
import maya

TaskKey = collections.namedtuple('TaskKey', 'task_arn task_definition_arn')
Task = collections.namedtuple(
    'Task',
    'task_key started_at completed success'
)


def _create_task_tuple_from_item(item):
    started_at = ""
    if 'startedAt' in item:
        started_at = maya.MayaDT.from_datetime(item['startedAt']).iso8601()

    return Task(
        TaskKey(
            item['task_arn'],
            item['task_definition_arn']
        ),
        started_at,
        item['completed'],
        item['success']
    )


def _get_tasks_from_dynamo(table):
    response = table.scan()
    return [_create_task_tuple_from_item(d) for d in response['Items']]


def _find_in_tasks(task_list, key):
    return [t for t in task_list if t.task_key == key][0]


def _compare_tasks(current_tasks, last_tasks):
    current_tasks_keys = set(
        [current_task.task_key
         for current_task in current_tasks])

    last_tasks_keys = set([last_task.task_key
                           for last_task in last_tasks])

    deletions_keys = last_tasks_keys - current_tasks_keys
    additions_keys = current_tasks_keys - last_tasks_keys
    remaining_keys = last_tasks_keys & current_tasks_keys

    deleted_tasks = [
        _find_in_tasks(last_tasks, deletion_key)
        for deletion_key in deletions_keys
    ]

    added_tasks = [
        _find_in_tasks(current_tasks, additions_key)
        for additions_key in additions_keys
    ]

    updated_tasks = [
        _find_in_tasks(current_tasks, remaining_key)
        for remaining_key in remaining_keys
    ]

    updated_tasks = set(current_tasks) - set(updated_tasks)

    return {
        'deletions': deleted_tasks,
        'additions': added_tasks,
        'updates': updated_tasks
    }


def _json_converter(o):
    if isinstance(o, datetime.datetime):
        return o.__str__()


def _put_task_in_dynamo(table, task):
    task_key = {
        'task_definition_arn': task.task_key.task_definition_arn,
        'task_arn': task.task_key.task_arn,
    }

    task_metadata = {
        'started_at': task.started_at,
        'completed': task.completed,
        'success': task.success
    }

    filtered_task_metadata = dict(
        (k, v) for k, v in task_metadata.items() if v
    )

    item = {**task_key, **filtered_task_metadata}

    return table.put_item(Item=item)


def _delete_task_in_dynamo(table, task):
    return table.delete_item(
        Key={
            'task_definition_arn': task.task_key.task_definition_arn,
            'task_arn': task.task_key.task_arn
        }
    )


def _update_task_in_dynamo(table, task):
    return table.update_item(
        Key={
            'task_arn': task.task_key.task_arn,
            'task_definition_arn': task.task_key.task_definition_arn
        },
        UpdateExpression="""
            SET started_at = :started_at,
                completed = :completed,
                success = :success
        """,
        ExpressionAttributeValues={
            ':started_at': task.started_at,
            ':completed': task.completed,
            ':success': task.success
        }
    )


def _run_operations(operations, table):
    return {
        "delete_results": [_delete_task_in_dynamo(table, task)
                           for task in operations["deletions"]],
        "put_results": [_put_task_in_dynamo(table, task)
                        for task in operations["additions"]],
        "update_results": [_update_task_in_dynamo(table, task)
                           for task in operations["updates"]]
    }


def _get_describe_tasks_response(ecs_client, cluster_name):
    list_tasks_response = ecs_client.list_tasks(cluster=cluster_name)

    return ecs_client.describe_tasks(
        cluster=cluster_name,
        tasks=list_tasks_response['taskArns']
    )


def _create_task_tuple_from_task(task):
    def _get_completed_successfully(task):
        exit_codes = [
            container['exitCode'] for container in task['containers'] if ('exitCode' in container)
        ]

        if len(task['containers']) == len(exit_codes):
            for exit_code in exit_codes:
                if exit_code != 0:
                    return False

                return True

        return None

    started_at = ""
    if 'startedAt' in task:
        started_at = maya.MayaDT.from_datetime(task['startedAt']).iso8601()

    completed = _get_completed_successfully(task) is not None

    success = False
    if completed:
        success = _get_completed_successfully(task)

    return Task(
        TaskKey(
            task['taskArn'],
            task['taskDefinitionArn']
        ),
        started_at,
        completed,
        success
    )


def main(event, _):
    print(f'event = {event!r}')

    table_name = os.environ["TABLE_NAME"]
    cluster_name = os.environ["CLUSTER_NAME"]

    ecs_client = boto3.client('ecs')
    dynamodb = boto3.resource('dynamodb')
    table = dynamodb.Table(table_name)

    describe_tasks_response = _get_describe_tasks_response(ecs_client, cluster_name)

    tasks_from_ecs = [_create_task_tuple_from_task(task) for task in describe_tasks_response['tasks']]
    tasks_from_dynamo = _get_tasks_from_dynamo(table)

    operations = _compare_tasks(tasks_from_ecs, tasks_from_dynamo)
    print(f'operations = {operations!r}')

    operation_resulsts = _run_operations(operations, table)
    print(f'operation_resulsts = {operation_resulsts!r}')
