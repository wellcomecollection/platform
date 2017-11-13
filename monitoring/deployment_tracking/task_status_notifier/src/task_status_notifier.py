#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Task Status Notifier

Provides SNS topic notifications for task status changes
"""

import os

import boto3

from wellcome_aws_utils.sns_utils import extract_json_message, publish_sns_message
from wellcome_aws_utils.dynamo_event import DynamoEvent, DynamoEventType


def main(event, _):
    print(f'event = {event!r}')

    sns_client = boto3.client('sns')

    task_stopped_topic = os.environ["TASK_STOPPED_TOPIC_ARN"]
    task_started_topic = os.environ["TASK_STARTED_TOPIC_ARN"]
    task_updated_topic = os.environ["TASK_UPDATED_TOPIC_ARN"]

    event = DynamoEvent(
        extract_json_message(event)
    )

    if event.event_type == DynamoEventType.INSERT:
        topic_arn = task_started_topic
        subject = 'STARTED'
        image = event.new_image(True)
    if event.event_type == DynamoEventType.REMOVE:
        topic_arn = task_stopped_topic
        subject = 'STOPPED'
        image = event.old_image(True)
    if event.event_type == DynamoEventType.MODIFY:
        topic_arn = task_updated_topic
        subject = 'MODIFIED'
        image = event.new_image(True)

    if (not topic_arn) or (not subject) or (not image):
        raise Exception(f'Unable to publish task status change for {event}')

    publish_sns_message(
        sns_client=sns_client,
        topic_arn=topic_arn,
        message=image,
        subject=subject
    )
