# -*- encoding: utf-8 -*-
"""
Publish a service schedule request for the reindexer to SNS.

This script is triggered by updates to the reindexer DynamoDB table.
"""

import os

import boto3

from wellcome_aws_utils.sns_utils import publish_sns_message



def main(event, _context):
    print(f'event = {event!r}')

    infra_bucket = os.environ['INFRA_BUCKET']
    target_topic = os.environ['TARGET_TOPIC']

    records = event['Records']
    assert len(records) == 1
    changed_object_key = records[0]['s3']['object']['key']
    assert changed_object_key == '/prod_api'

    client = boto3.client('s3')
    prod_api = client.get_object(
        Bucket=infra_bucket,
        Key='prod_api'
    )['Body'].read()

    sns_client = boto3.client('sns')


    def update_api_count(service, count):
        publish_sns_message(
            sns_client=sns_client,
            topic_arn=target_topic,
            message={
                'cluster': 'api_cluster',
                'service': f'api_{service}_v1',
                'count': count
            },
            subject='switching-prod-api'
        )


    if prod_api == b'romulus':
        update_api_count(service='romulus', count=3)
        update_api_count(service='remus', count=1)
    elif prod_api == b'remus':
        update_api_count(service='remus', count=3)
        update_api_count(service='romulus', count=1)
    else:
        raise RuntimeError("Unrecognised prod API: %r" % prod_api)
