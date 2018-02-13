# -*- encoding: utf-8 -*-

import os

import boto3

from wellcome_aws_utils.sns_utils import (
    extract_sns_messages_from_lambda_event, publish_sns_message
)


def main(event, _ctxt=None, sns_client=None):
    print(f'event={event!r}')

    sns_client = sns_client or boto3.client('sns')
    topic_arn = os.environ['TOPIC_ARN']

    for sns_event in extract_sns_messages_from_lambda_event(event):
        image = sns_event.message

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
            subject='source: reindex_job_creator.main'
        )
