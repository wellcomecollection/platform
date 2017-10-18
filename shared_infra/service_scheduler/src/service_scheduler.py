#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Publish a service scheduler to SNS.

This script runs on a fixed schedule to send an SNS notification to start
one of our adapters.  It receives a blob of JSON from a CloudWatch timed
event, and publishes that to the service scheduler topic.
"""

from wellcome_lambda_utils.sns_utils import publish_sns_message


def main(event, _):
    print(f'event = {event!r}')
    message = {
        'cluster': event['cluster'],
        'service': event['service'],
        'desired_count': event['desired_count'],
    }
    publish_sns_message(topic_arn=event['topic_arn'], message=message)
