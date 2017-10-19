#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Receives DynamoDB events and publishes the new image from
the event to an SNS topic
"""
import json
import os

import boto3

from wellcome_lambda_utils.dynamo_utils import DynamoImageFactory
from wellcome_lambda_utils.sns_utils import publish_sns_message


def _publish_image(record, topic_arn, sns_client):
    new_image = record.simplified_new_image

    print(new_image)

    if new_image is not None:
        publish_sns_message(
            topic_arn=topic_arn,
            message=new_image,
            sns_client=sns_client
        )


def main(event, _):
    print(f'Received event:\n{event}')

    sns_client = boto3.client('sns')
    stream_topic_map = json.loads(os.environ["STREAM_TOPIC_MAP"])

    for record in DynamoImageFactory.create(event):
        print(record)

        topic_arn = stream_topic_map[record.source_arn]

        _publish_image(
            record,
            topic_arn,
            sns_client
        )
