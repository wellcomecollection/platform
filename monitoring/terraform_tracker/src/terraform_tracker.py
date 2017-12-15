# -*- encoding: utf-8 -*-
"""
Sends Slack notifications for 'terraform apply' invocations.
"""

import json
import os
import sys

from botocore.vendored import requests


def main(event, context):
    print(f'event = {event!r}')

    slack_webhook = os.environ['SLACK_WEBHOOK']
    message = event['Records'][0]['Sns']

    if message['Subject'] != 'terraform-apply-notification':
        print("Ignoring this event")
        sys.exit(0)

    data = json.loads(message['Message'])

    username = data['username']
    stack = {
        'platform.tfstate': 'catalogue_api',
        'platform-pipeline.tfstate': 'catalogue_pipeline',
        'platform-lambda.tfstate': 'shared_infra',
    }.get(data["stack"], data["stack"])

    stack = stack.replace('platform-', '').replace('terraform/', '')
    if stack.endswith('.tfstate'):
        stack = stack[:-len('.tfstate')]

    slack_data = {
        'username': 'terraform-apply',
        'icon_emoji': ':terraform:',
        'text': f'{username} has run "terraform apply" in the {stack} stack.'
    }

    print('Sending message %s' % json.dumps(slack_data))

    response = requests.post(
        slack_webhook,
        data=json.dumps(slack_data),
        headers={'Content-Type': 'application/json'}
    )
    response.raise_for_status()
