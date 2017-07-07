# -*- encoding: utf-8 -*-

from datetime import datetime
import json
import pprint

import boto3


class EnhancedJSONEncoder(json.JSONEncoder):
    def default(self, o):
        if isinstance(o, datetime):
            return o.isoformat()

        return json.JSONEncoder.default(self, o)


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
            'default': json.dumps(message, cls=EnhancedJSONEncoder)
        })
    )
    print(f'SNS response:\n{pprint.pformat(resp)}')
    assert resp['ResponseMetadata']['HTTPStatusCode'] == 200
