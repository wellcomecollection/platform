#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Sends slack notifications for alarms events
"""

import json
import os

from botocore.vendored import requests


class Alarm:
    def __init__(self, json_message):
        self.message = json.loads(json_message)

    @property
    def name(self):
        return self.message['AlarmName']

    @property
    def namespace(self):
        return self.message['Trigger']['Namespace']

    @property
    def metric_name(self):
        return self.message['Trigger']['MetricName']

    @property
    def dimensions(self):
        return self.message['Trigger']['Dimensions']

    @property
    def state_reason(self):
        return self.message['NewStateReason']

    @property
    def state_change_time(self):
        return self.message['StateChangeTime']


def main(event, _):
    print(f'event = {event!r}')
    alarm = Alarm(event['Records'][0]['Sns']['Message'])

    slack_data = {'username': 'cloudwatch-alert',
                  "icon_emoji": ":slack:",
                  "attachments": [{
                      'color': 'danger',
                      'fallback': alarm.name,
                      "title": alarm.name,
                      "fields": [
                          {
                              "title": "Metric",
                              "value": f"{alarm.namespace}/{alarm.metric_name}"
                          },
                          {
                              "title": "Dimensions",
                              "value": repr(alarm.dimensions)
                          },
                          {
                              "title": "Reason",
                              "value": alarm.state_reason
                          },
                          {
                              "title": "Timestamp",
                              "value": alarm.state_change_time
                          }
                      ]
                  }]}

    response = requests.post(
        os.environ["SLACK_INCOMING_WEBHOOK"],
        data=json.dumps(slack_data),
        headers={'Content-Type': 'application/json'}
    )
    response.raise_for_status()
