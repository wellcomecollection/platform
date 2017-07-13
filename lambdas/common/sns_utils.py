# -*- encoding: utf-8 -*-

from datetime import datetime
import decimal

import boto3
import simplejson as json


class EnhancedJSONEncoder(json.JSONEncoder):
    def default(self, o):
        if isinstance(o, datetime):
            return o.isoformat()

        if isinstance(o, decimal.Decimal):
            # wanted a simple yield str(o) in the next line,
            # but that would mean a yield on the line with super(...),
            # which wouldn't work (see my comment below), so...
            return (str(o) for o in [o])

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
            'default': json.dumps(
                message,
                cls=EnhancedJSONEncoder,
                iterable_as_array=True
            )
        })
    )
    print(f'SNS response = {resp!r}')
    assert resp['ResponseMetadata']['HTTPStatusCode'] == 200
