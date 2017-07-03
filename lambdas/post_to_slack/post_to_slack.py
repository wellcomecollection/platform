#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""

"""

import json
import pprint
import os
from botocore.vendored import requests


def main(event, _):
    print(f'Received event:\n{pprint.pformat(event)}')
    message = event['Records'][0]['Sns']['Message']
    message_data = json.loads(message)
    webhook_url = os.environ["SLACK_INCOMING_WEBHOOK"]

    slack_data = {'username': 'cloudwatch-alert', "icon_emoji": ":slack:", "attachments": [{
        'color': 'danger',
        'fallback': message_data['AlarmName'],
        "title": message_data['AlarmName'],
        "fields": [
            {
                "title": "Metric",
                "value": f"{message_data['Trigger']['Namespace']}/{message_data['Trigger']['MetricName']}"
            },
            {
                "title": "Dimensions",
                "value": f"{pprint.pformat(message_data['Trigger']['Dimensions'])}"
            },
            {
                "title": "Reason",
                "value": message_data['NewStateReason']
            }
        ]
    }
    ]}

    response = requests.post(
        webhook_url, data=json.dumps(slack_data),
        headers={'Content-Type': 'application/json'}
    )
    if response.status_code != 200:
        raise ValueError(
            'Request to slack returned an error %s, the response is:\n%s'
            % (response.status_code, response.text)
        )
