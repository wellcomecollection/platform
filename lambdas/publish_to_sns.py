#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Publish a message to SNS.

This script runs on a fixed schedule to send an SNS notification to start
one of our adapters.
"""

import json

import boto3


def publish_sns_message(topic_arn, **kwargs):
    """
    Given a topic ARN and a series of key-value pairs, publish the key-value
    data to the SNS topic.
    """
    sns = boto3.client('sns')
    resp = sns.publish(
        TopicArn=topic_arn,
        MessageStructure='json',
        Message=json.dumps({
            'default': json.dumps(kwargs)
        })
    )
    print(resp)
    assert resp['ResponseMetadata']['HTTPStatusCode'] == 200


def main(event, context):
    # extract the topic arn, message data from event/context
    publish_sns_message(topic_arn, **event)
