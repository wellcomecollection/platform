# -*- encoding: utf-8 -*-
"""
Sends Slack notifications for ECR pushes.
"""

import json
import os

from botocore.vendored import requests


def main(event, context):
    print(f'event = {event!r}')

    slack_webhook = os.environ['SLACK_WEBHOOK']

    message = event['Records'][0]['Sns']

    data = json.loads(message['Message'])

    iam_user = data['iam_user']
    project = data['project']
    git_branch = data['git_branch']
    commit_id = data['commit_id']
    commit_msg = data['commit_msg']

    lines = [
        f'{iam_user} has pushed a new *{project}* image to ECR.',
        f'Git branch: {git_branch} ({commit_id})',
        f'Commit message: {commit_msg!r}',
    ]

    slack_data = {
        'username': 'ecr-pushes',
        'icon_emoji': ':ecr:',
        'attachments': [{
            'color': '#F58536',
            'fields': [{'value': l} for l in lines]
        }]
    }

    print(f'Sending message {slack_data!r}')

    response = requests.post(
        slack_webhook,
        data=json.dumps(slack_data),
        headers={'Content-Type': 'application/json'}
    )
    response.raise_for_status()
