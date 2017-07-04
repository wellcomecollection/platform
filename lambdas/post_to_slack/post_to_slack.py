#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Sends slack notifications for alarms events
"""

import json
import pprint
import os

from botocore.vendored import requests


def main(event, _):
    print(f'Received event:\n{pprint.pformat(event)}')
    message = event['Records'][0]['Sns']['Message']
    message_data = json.loads(message)
    alarm_name = message_data['AlarmName']
    namespace = message_data['Trigger']['Namespace']
    metric_name = message_data['Trigger']['MetricName']
    dimensions = message_data['Trigger']['Dimensions']
    state_reason = message_data['NewStateReason']

    slack_data = {'username': 'cloudwatch-alert',
                  "icon_emoji": ":slack:",
                  "attachments": [{
                      'color': 'danger',
                      'fallback': alarm_name,
                      "title": alarm_name,
                      "fields": [
                          {
                              "title": "Metric",
                              "value": f"{namespace}/{metric_name}"
                          },
                          {
                              "title": "Dimensions",
                              "value": f"{pprint.pformat(dimensions)}"
                          },
                          {
                              "title": "Reason",
                              "value": state_reason
                          }
                      ]
                  }]}

    response = requests.post(
        os.environ["SLACK_INCOMING_WEBHOOK"],
        data=json.dumps(slack_data),
        headers={'Content-Type': 'application/json'}
    )
    if response.status_code != 200:
        raise ValueError(
            'Request to slack returned an error %s, the response is:\n%s'
            % (response.status_code, response.text)
        )
