# -*- encoding: utf-8 -*-
"""
Sends Slack notifications for 'terraform apply' invocations.
"""

import json
import os

from botocore.vendored import requests


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


def main(event, context):
    print(f'event = {event!r}')

    infra_bucket = os.environ['INFRA_BUCKET']
    slack_webhook = os.environ['SLACK_WEBHOOK']
    bitly_access_token = os.environ['BITLY_ACCESS_TOKEN']

    message = event['Records'][0]['Sns']

    if message['Subject'] != 'terraform-apply-notification':
        print("Ignoring this event")
        return

    data = json.loads(message['Message'])

    username = data['username']
    key = data['key']

    key_url = f'https://console.aws.amazon.com/s3/object/{infra_bucket}/{key}?region=eu-west-1&tab=overview'
    display_url = to_bitly(key_url, access_token=bitly_access_token)

    stack = {
        'platform.tfstate': 'catalogue_api',
        'platform-pipeline.tfstate': 'catalogue_pipeline',
        'platform-lambda.tfstate': 'shared_infra',
    }.get(data["stack"], data["stack"])

    stack = stack.replace('platform-', '').replace('terraform/', '')
    if stack.endswith('.tfstate'):
        stack = stack[:-len('.tfstate')]

    message = f'{username} has run "terraform apply" in the {stack} stack.'

    try:
        git_branch = data['git_branch']
        git_commit = data['git_commit']
    except KeyError:
        # The user is running an older version of the Terraform Docker image
        # which doesn't provide Git information.
        pass
    else:
        message += f'\nGit branch: {git_branch} ({git_commit[:7]}).'

    slack_data = {
        'username': 'terraform-tracker',
        'icon_emoji': ':terraform:',
        'attachments': [{
            'color': '#5C4EE5',
            'fields': [
                {'value': message},
                {'value': display_url}
            ]
        }]
    }

    print('Sending message %s' % json.dumps(slack_data))

    response = requests.post(
        slack_webhook,
        data=json.dumps(slack_data),
        headers={'Content-Type': 'application/json'}
    )
    response.raise_for_status()
