#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Receives DynamoDB events and publishes the new image from
the event to an SNS topic
"""
import json
import os

from utils.dynamo_utils import DynamoImageFactory
from utils.sns_utils import publish_sns_message


def _publish_image(record, topic_arn):
    new_image = record.simplified_new_image

    print(new_image)

    if new_image is not None:
        publish_sns_message(topic_arn, new_image)


def main(event, _):
    print(f'Received event:\n{event}')

    stream_topic_map = json.loads(os.environ["STREAM_TOPIC_MAP"])

    for record in DynamoImageFactory.create(event):

        print(record)

        topic_arn = stream_topic_map[record.source_arn]

        _publish_image(
            record,
            topic_arn
        )
