#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
This is used to update the task registry from task status change topic in the monitoring stack

"""

import os

import boto3

from wellcome_aws_utils.sns_utils import extract_sns_messages_from_lambda_event


def update_task_in_dynamo(table, task):
    return table.update_item(
        Key={
            'task_definition_arn': task['task_definition_arn'],
            'task_arn': task['task_arn']
        },
        ConditionExpression="attribute_exists(task_definition_arn)",
        UpdateExpression="""
            SET started_at = :started_at,
                completed = :completed,
                success = :success
        """,
        ExpressionAttributeValues={
            ':started_at': task['started_at'],
            ':completed': task['completed'],
            ':success': task['success']
        }

    )


def main(event,_):
    print(f'event = {event!r}')

    table_name = os.environ["TABLE_NAME"]
    dynamodb = boto3.resource('dynamodb')
    table = dynamodb.Table(table_name)

    messages = extract_sns_messages_from_lambda_event(event)

    for message in messages:
        task = message['message']
        update_task_in_dynamo(table, task)

