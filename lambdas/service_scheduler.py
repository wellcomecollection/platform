#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Publish a service scheduler to SNS.

This script runs on a fixed schedule to send an SNS notification to start
one of our adapters.
"""

import json

import boto3


def publish_sns_message(topic_arn, cluster, service, desired_count):
    """
    Given a topic ARN and a series of key-value pairs, publish the key-value
    data to the SNS topic.
    """
    sns = boto3.client('sns')
    resp = sns.publish(
        TopicArn=topic_arn,
        MessageStructure='json',
        Message=json.dumps({
            'default': json.dumps({
                'cluster': cluster,
                'service': service,
                'desired_count': desired_count
            })
        })
    )
    print('SNS response: %r' % resp)
    assert resp['ResponseMetadata']['HTTPStatusCode'] == 200


def main(event, _):
    print('Received event: %r' % event)
    publish_sns_message(
        topic_arn=event['topic_arn'],
        cluster=event['cluster'],
        service=event['service'],
        desired_count=event['desired_count']
    )
