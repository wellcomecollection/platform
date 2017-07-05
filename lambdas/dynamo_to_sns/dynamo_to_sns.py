#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""

"""
import json
import os

from sns_utils import publish_sns_message


def main(event, _):
    print(f'Received event:\n{event}')
    stream_topic_map = json.loads(os.environ["STREAM_TOPIC_MAP"])
    new_image = event['Records'][0]['dynamodb']['NewImage']
    topic_arn = stream_topic_map[event['Records'][0]['eventSourceARN']]
    publish_sns_message(topic_arn,new_image)
