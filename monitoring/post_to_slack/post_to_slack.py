#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Sends slack notifications for alarms events
"""

import collections
import datetime as dt
import json
import os
import re
from urllib.parse import quote

import boto3
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


class CloudWatchException(Exception):
    pass


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

    # Sometimes there's enough data in the alarm to make an educated guess
    # about useful CloudWatch logs to check, so we include that in the alarm.
    # The methods and properties below pull out the relevant info.

    @property
    def cloudwatch_search_terms(self):
        """
        Returns a list of potentially useful search terms in CloudWatch.
        """
        if self.name == 'loris-alb-target-500-errors':
            return ['"HTTP/1.0 500"']
        elif self.name.startswith('lambda'):
            return ['Traceback', 'Task timed out after']
        elif self.name.startswith('api_'):
            return ['"HTTP 500"']
        else:
            return []

    @property
    def cloudwatch_log_group(self):
        """
        Returns the CloudWatch log group most likely to contain the error.
        """
        if self.name == 'loris-alb-target-500-errors':
            return 'platform/loris'
        elif self.name.startswith('lambda'):
            lambda_name = self.name.split('-')[1]
            return f'/aws/lambda/{lambda_name}'
        elif self.name == 'api_romulus_v1-alb-target-500-errors':
            return 'platform/api_romulus_v1'
        elif self.name == 'api_remus_v1-alb-target-500-errors':
            return 'platform/api_remus_v1'
        else:
            raise CloudWatchException(
                "I don't know where to look for logs for %r" % self.name
            )

    @property
    def cloudwatch_timeframe(self):
        """
        Try to work out a likely timeframe for CloudWatch errors.
        """
        timeframe = collections.namedtuple('timeframe', ['start', 'end'])
        match = DATAPOINT_RE.search(self.state_reason)
        if match is None:
            raise CloudWatchException(
                "I can't find a timeframe for reason %r" % self.reason
            )

        timestamp = match.group('timestamp')
        time = dt.datetime.strptime(timestamp, '%d/%m/%y %H:%M:%S')
        start = time - dt.timedelta(seconds=300)
        end = time + dt.timedelta(seconds=300)

        return timeframe(start=start, end=end)

    def cloudwatch_urls(self):
        """
        Return some CloudWatch URLs that might be useful to check.
        """
        try:
            group = self.cloudwatch_log_group
            timeframe = self.cloudwatch_timeframe
            return [
                self._build_cloudwatch_url(
                    search_term=search_term, group=group, timeframe=timeframe
                )
                for search_term in self.cloudwatch_search_terms
            ]
        except CloudWatchException as exc:
            print(f'Error in cloudwatch_urls: {exc}')
            return []

    def cloudwatch_messages(self):
        """
        Try to find some CloudWatch messages that might be relevant.
        """
        client = boto3.client('logs')

        messages = []

        try:
            # CloudWatch wants these parameters specified as seconds since
            # 1 Jan 1970 00:00:00, so convert to that first.
            timeframe = self.cloudwatch_timeframe
            epoch = dt.datetime(1970, 1, 1, 0, 0, 0)
            startTime = int((timeframe.start - epoch).total_seconds() * 1000)
            endTime = int((timeframe.end - epoch).total_seconds() * 1000)

            # We only get the first page of results.  If there's more than
            # one page, we have so many errors that not getting them all
            # in the Slack alarm is the least of our worries!
            for term in self.cloudwatch_search_terms:
                resp = client.filter_log_events(
                    logGroupName=self.cloudwatch_log_group,
                    startTime=startTime,
                    endTime=endTime,
                    filterPattern=term
                )
                messages.extend([e['message'] for e in resp['events']])

        except Exception as exc:
            print(f'Error in cloudwatch_messages: {exc}')

        return messages

    @staticmethod
    def _build_cloudwatch_url(search_term, group, timeframe):
        return (
            'https://eu-west-1.console.aws.amazon.com/cloudwatch/home'
            '?region=eu-west-1'
            f'#logEventViewer:group={group};'

            # Look for strings matching 'HTTP/1.0 500'
            f'filter={quote(search_term)};'

            # And add the date parameters to filter to the exact time
            f'start={timeframe.start.strftime("%Y-%m-%dT%H:%M:%SZ")};'
            f'end={timeframe.end.strftime("%Y-%m-%dT%H:%M:%SZ")};'
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


def simplify_message(message):
    """
    Sometimes a CloudWatch message includes information that we don't want
    to appear in Slack -- e.g. date/time.

    This function tries to strip out extra bits from the message, so we get
    a tight and focused error appearing in Slack.
    """
    # Scala messages have a prefix that gives us a timestamp and thread info:
    #
    #     14:02:47.249 [ForkJoinPool-2-worker-17]
    #
    # Bin it!
    message = re.sub(
        r'\d{2}:\d{2}:\d{2}\.\d{3} \[ForkJoinPool-\d+-worker-\d+\] ', '',
        message
    )

    # Loris messages have a bunch of origin and UWSGI information as a prefix:
    #
    #     [pid: 86|app: 0|req: 195/3682] 172.17.0.5 () {40 vars in 879
    #       bytes} [Tue Oct 10 19:37:06 2017]
    #
    # Discard it!
    message = re.sub(
        r'\[pid: \d+\|app: \d+\|req: \d+/\d+\] '
        r'\d+\.\d+\.\d+\.\d+ \(\) '
        r'{\d+ vars in \d+ bytes} '
        r'\[[A-Za-z0-9: ]+\]', '', message
    )

    # Loris messages also have an uninteresting suffix:
    #
    #     3 headers in 147 bytes (1 switches on core 0)
    #
    # Throw it away!
    message = re.sub(
        r'\d+ headers in \d+ bytes \(\d+ switches on core \d+\)', '', message
    )

    # Lambda timeouts have an opaque prefix:
    #
    #     2017-10-12T13:18:31.917Z d1fdfca5-af4f-11e7-a100-030f2a39c6f6 Task
    #     timed out after 10.01 seconds
    #
    # Drop it!
    message = re.sub(
        r'\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z '
        r'[0-9a-f-]+ (?=Task timed out)', '', message
    )

    return message.strip()


def main(event, _):
    print(f'event = {event!r}')

    bitly_access_token = os.environ['BITLY_ACCESS_TOKEN']

    alarm = Alarm(event['Records'][0]['Sns']['Message'])

    slack_data = {
        'username': 'cloudwatch-alert',
        'icon_emoji': ':rotating_light:',
        'attachments': [
            {
                'color': 'danger',
                'fallback': alarm.name,
                'title': alarm.name,
                'fields': [{
                    'value': alarm.human_reason() or alarm.state_reason
                }]
            }
        ]
    }

    messages = alarm.cloudwatch_messages()
    if messages:
        cloudwatch_message_str = '\n'.join(set([
            simplify_message(m) for m in messages
        ]))
        slack_data['attachments'][0]['fields'].append({
            'title': 'CloudWatch messages',
            'value': cloudwatch_message_str
        })

    cloudwatch_urls = alarm.cloudwatch_urls()
    if cloudwatch_urls:
        cloudwatch_url_str = ' / '.join([
            to_bitly(url=url, access_token=bitly_access_token)
            for url in cloudwatch_urls
        ])
        slack_data['attachments'][0]['fields'].append({
            'value': cloudwatch_url_str
        })

    print('Sending message %s' % json.dumps(slack_data))

    response = requests.post(
        os.environ["SLACK_INCOMING_WEBHOOK"],
        data=json.dumps(slack_data),
        headers={'Content-Type': 'application/json'}
    )
    response.raise_for_status()
