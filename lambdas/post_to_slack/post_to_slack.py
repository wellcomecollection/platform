#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Sends slack notifications for alarms events
"""

import datetime as dt
import json
import os
import re
from urllib.parse import quote

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


def guess_cloudwatch_url(alarm):
    """
    Sometimes there's enough data in the alarm to make an educated guess
    about useful CloudWatch logs to check.  This function tries to do that.
    """
    # We can get the time from the datapoint in the reason, which is
    # typically of the form
    #
    #     Threshold Crossed: 1 datapoint [1.0 (11/08/18 10:55:00)] was
    #     greater than or equal to the threshold (1.0).
    #
    # So try to get the timestamp.
    match = re.search(
        r'\[(?P<datapoint>[0-9.]+) \((?P<timestamp>[0-9]{2}/[0-9]{2}/[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2})\)\]',
        alarm.state_reason
    )
    if match is None:
        return

    timestamp = match.group('timestamp')
    time = dt.datetime.strptime(timestamp, '%d/%m/%y %H:%M:%S')
    start = time - dt.timedelta(seconds=300)
    end = time + dt.timedelta(seconds=300)

    if alarm.name == 'loris-alb-target-500-errors':
        group = 'platform/loris'
        search_term = '"HTTP/1.0 500"'
    elif alarm.name.startswith('lambda'):
        lambda_name = alarm.name.split('-')[1]
        group = f'/aws/lambda/{lambda_name}'
        search_term = 'Traceback'
    else:
        return

    return (
        'https://eu-west-1.console.aws.amazon.com/cloudwatch/home'
        '?region=eu-west-1'
        f'#logEventViewer:group={group};'

        # Look for strings matching 'HTTP/1.0 500'
        f'filter={quote(search_term)};'

        # And add the date parameters to filter to the exact time
        f'start={start.strftime("%Y-%m-%dT%H:%M:%SZ")};'
        f'end={end.strftime("%Y-%m-%dT%H:%M:%SZ")};'
    )


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

    cloudwatch_url = guess_cloudwatch_url(alarm)
    if cloudwatch_url is not None:
        slack_data['attachments'][0]['fields'].append({
            'title': 'CloudWatch URL',
            'value': cloudwatch_url
        })

    response = requests.post(
        os.environ["SLACK_INCOMING_WEBHOOK"],
        data=json.dumps(slack_data),
        headers={'Content-Type': 'application/json'}
    )
    response.raise_for_status()
