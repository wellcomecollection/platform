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


# Alarm reasons are sometimes of the form:
#
#     Threshold Crossed: 1 datapoint [1.0 (11/08/18 10:55:00)] was
#     greater than or equal to the threshold (1.0).
#
# This regex is meant to match the datapoint in square brackets.
DATAPOINT_RE = re.compile(r'''
    \[
      (?P<value>[0-9.]+)\s
      \(
        (?P<timestamp>[0-9]{2}/[0-9]{2}/[0-9]{2}\s[0-9]{2}:[0-9]{2}:[0-9]{2})
      \)
    \]
''', flags=re.VERBOSE)


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

    def human_reason(self):
        """
        Try to return a more human-readable explanation for the alarm.
        """
        match = DATAPOINT_RE.search(self.state_reason)
        if match is None:
            return

        value = int(float(match.group('value')))

        timestamp = match.group('timestamp')
        time = dt.datetime.strptime(timestamp, '%d/%m/%y %H:%M:%S')
        display_time = time.strftime('at %H:%M:%S on %d %b %Y')

        if self.name.endswith('-alb-target-500-errors'):
            if self.name.startswith('loris'):
                service = 'Loris'
            elif self.name.startswith('api_'):
                service = 'the API'
            else:
                return

            if value == 1:
                return f'The ALB spotted a 500 error in {service} {display_time}.'
            else:
                return f'The ALB spotted multiple 500 errors ({value}) in {service} {display_time}.'

    def cloudwatch_url(self):
        """
        Sometimes there's enough data in the alarm to make an educated guess
        about useful CloudWatch logs to check.  This method tries to do that.
        """
        match = DATAPOINT_RE.search(self.state_reason)
        if match is None:
            return

        timestamp = match.group('timestamp')
        time = dt.datetime.strptime(timestamp, '%d/%m/%y %H:%M:%S')
        start = time - dt.timedelta(seconds=300)
        end = time + dt.timedelta(seconds=300)

        if self.name == 'loris-alb-target-500-errors':
            group = 'platform/loris'
            search_term = '"HTTP/1.0 500"'
        elif self.name.startswith('lambda'):
            lambda_name = self.name.split('-')[1]
            group = f'/aws/lambda/{lambda_name}'

            # For Lambdas, we could have a Traceback, or we could have a
            # timeout.  Return both, because there's no way to do an OR search
            # in the CloudWatch console.
            url1 = self._build_cloudwatch_url('Traceback', group, start, end)
            url2 = self._build_cloudwatch_url('Task timed out after', group, start, end)
            return f'{url1} / {url2}'

        elif self.name == 'api_romulus_v1-alb-target-500-errors':
            group = 'platform/api_romulus_v1'
            search_term = '"HTTP 500"'
        elif self.name == 'api_remus-alb-target-500-errors':
            group = 'platform/api_remus'
            search_term = '"HTTP 500"'
        else:
            return

        return self._build_cloudwatch_url(search_term, group, start, end)

    @staticmethod
    def _build_cloudwatch_url(search_term, group, start, end):
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


def to_bitly(url, access_token):
    """
    Try to shorten a URL with bit.ly.  If it fails, just return the
    original URL.
    """
    def _to_bity_single_url(url):
        resp = requests.get(
            'https://api-ssl.bitly.com/v3/user/link_save',
            params={'access_token': access_token, 'longUrl': url}
        )
        try:
            return resp.json()['data']['link_save']['link']
        except KeyError:
            return url

    return ' / '.join([_to_bity_single_url(u) for u in url.split(' / ')])


def main(event, _):
    print(f'event = {event!r}')

    bitly_access_token = os.environ['BITLY_ACCESS_TOKEN']

    alarm = Alarm(event['Records'][0]['Sns']['Message'])

    slack_data = {'username': 'cloudwatch-alert',
                  "icon_emoji": ":rotating_light:",
                  "attachments": [{
                      'color': 'danger',
                      'fallback': alarm.name,
                      "title": alarm.name,
                      "fields": [
                          {
                              "title": "Reason",
                              "value": alarm.human_reason() or alarm.state_reason
                          },
                      ]
                  }]}

    cloudwatch_url = alarm.cloudwatch_url()
    if cloudwatch_url is not None:
        cloudwatch_url = to_bitly(
            url=cloudwatch_url,
            access_token=bitly_access_token
        )
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
