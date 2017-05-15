#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Publish a service scheduler to SNS.

This script runs on a fixed schedule to send an SNS notification to start
one of our adapters.
"""

from sns_utils import publish_sns_message


def main(event, _):
    print(f'Received event: {event!r}')
    publish_sns_message(
        topic_arn=event['topic_arn'],
        cluster=event['cluster'],
        service=event['service'],
        desired_count=event['desired_count']
    )
