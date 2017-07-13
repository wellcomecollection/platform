#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Receives DynamoDB events and publishes the new image from
the event to an SNS topic
"""
import json
import os

from dynamo_utils import DynamoEvent
from sns_utils import publish_sns_message


def main(event, _):
    print(f'Received event:\n{event}')
    dynamo_event = DynamoEvent(event)
    stream_topic_map = json.loads(os.environ["STREAM_TOPIC_MAP"])
    topic_arn = stream_topic_map[dynamo_event.source_arn]

    print(dynamo_event.simplified_new_image)

    publish_sns_message(topic_arn, dynamo_event.simplified_new_image)
