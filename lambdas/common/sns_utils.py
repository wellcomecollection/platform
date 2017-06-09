# -*- encoding: utf-8 -*-

import json

import boto3


def publish_sns_message(topic_arn, message):
    """
    Given a topic ARN and a series of key-value pairs, publish the key-value
    data to the SNS topic.
    """
    sns = boto3.client('sns')
    resp = sns.publish(
        TopicArn=topic_arn,
        MessageStructure='json',
        Message=json.dumps({
            'default': json.dumps(message)
        })
    )
    print(f'SNS response: {resp!r}')
    assert resp['ResponseMetadata']['HTTPStatusCode'] == 200
