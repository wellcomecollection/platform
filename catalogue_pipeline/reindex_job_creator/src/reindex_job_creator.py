# -*- encoding: utf-8 -*-

import os

import boto3

from wellcome_aws_utils.dynamo_event import DynamoEvent
from wellcome_aws_utils.sns_utils import publish_sns_message


def main(event, _ctxt):
    print(f'event={event!r}')

    sns_client = boto3.client('sns')
    topic_arn = os.environ['TOPIC_ARN']

    for record in event['Records']:
        image = record['dynamodb']['NewImage']

        if image['desiredVersion'] > image['currentVersion']:
            print(f"{image['desiredVersion']} > {image['currentVersion']}, creating job")
        else:
            print(f"{image['desiredVersion']} <= {image['currentVersion']}, nothing to do")
            continue

        message = {
            'shardId': image['shardId'],
            'desiredVersion': image['desiredVersion'],
        }

        publish_sns_message(
            sns_client=sns_client,
            topic_arn=topic_arn,
            message=message,
            subject='Reindex job from reindex_job_creator'
        )
