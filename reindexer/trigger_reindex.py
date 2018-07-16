#!/usr/bin/env python
# -*- encoding: utf-8
"""
Create/update reindex shards in the reindex shard tracker table.

Usage: trigger_reindex.py --source=<SOURCE_NAME> --reason=<REASON>
       trigger_reindex.py -h | --help

Options:
  --source=<SOURCE_NAME>    Name of the source you want to reindex.
  --reason=<REASON>         An explanation of why you're running this reindex.
                            This will be printed in the Slack alert.
  -h --help                 Print this help message

"""

import datetime as dt
import json
import os
import subprocess
import sys

import boto3
import docopt
import hcl
import requests
import tqdm
from wellcome_aws_utils.sns_utils import publish_sns_message

from dynamodb_capacity_helpers import (
    get_dynamodb_max_table_capacity,
    get_dynamodb_max_gsi_capacity,
    set_dynamodb_table_capacity,
    set_dynamodb_gsi_capacity
)


# Reindex shards are added by a "reindex_shard_generator" Lambda.
# Import the utility code that assigns reindex shards.
ROOT = subprocess.check_output(
    ['git', 'rev-parse', '--show-toplevel']).decode('utf8').strip()
sys.path.append(os.path.join(ROOT, 'reindexer/reindex_shard_generator/src'))

from reindex_shard_config import get_number_of_shards  # noqa


TOPIC_NAME = 'reindex_jobs'


def all_shard_ids(source_name):
    """
    Generates all the shard IDs in a given source name.

    e.g. miro/1, miro/2, miro/3, ...
    """
    count = get_number_of_shards(source_name=source_name)

    for shard_index in range(count):
        yield f'{source_name}/{shard_index}'


def all_messages(shard_ids, desired_version):
    """
    Generates all the messages to be sent to SNS.
    """
    for s_id in shard_ids:
        yield {
            'shardId': s_id,
            'desiredVersion': desired_version
        }


def publish_messages(sns_client, topic_arn, messages):
    """Publish a sequence of messages to an SNS topic."""
    for m in tqdm.tqdm(messages):
        publish_sns_message(
            sns_client=sns_client,
            topic_arn=topic_arn,
            message=m,
            subject=f'source: {__file__}'
        )


def post_to_slack(source_name, reason):
    """
    Posts a message about the reindex in Slack, so we can track them.
    """
    # Get the name of the current user.
    iam = boto3.client('iam')
    username = iam.get_user()['User']['UserName']

    # Get the non-critical Slack token.
    s3 = boto3.client('s3')
    tfvars_obj = s3.get_object(
        Bucket='wellcomecollection-platform-infra',
        Key='terraform.tfvars'
    )
    tfvars_body = tfvars_obj['Body'].read()
    tfvars = hcl.loads(tfvars_body)

    webhook_url = tfvars['non_critical_slack_webhook']

    message = f'*{username}* started a reindex in *{source_name}*\nReason: *{reason}*'

    slack_data = {
        'username': 'reindex-tracker',
        'icon_emoji': ':dynamodb:',
        'color': '#2E72B8',
        'title': 'reindexer',
        'fields': [{'value': message}]
    }

    resp = requests.post(
        webhook_url,
        data=json.dumps(slack_data),
        headers={'Content-Type': 'application/json'}
    )
    resp.raise_for_status()


def build_topic_arn(topic_name):
    """Given a topic name, return the topic ARN."""
    # https://stackoverflow.com/a/37723278/1558022
    sts_client = boto3.client('sts')
    account_id = sts_client.get_caller_identity().get('Account')

    return f'arn:aws:sns:eu-west-1:{account_id}:{topic_name}'


def main():
    args = docopt.docopt(__doc__)

    source_name = args['--source']
    reason = args['--reason']

    # We use the current timestamp for the reindex version -- this allows
    # us to easily trace when a reindex was triggered.
    desired_version = int(dt.datetime.utcnow().timestamp())
    print(f'Updating all shards in {source_name} to {desired_version}')

    post_to_slack(source_name=source_name, reason=reason)

    shard_ids = all_shard_ids(source_name=source_name)
    messages = all_messages(
        shard_ids=shard_ids,
        desired_version=desired_version
    )

    topic_arn = build_topic_arn(topic_name=TOPIC_NAME)

    sns_client = boto3.client('sns')
    publish_messages(
        sns_client=sns_client,
        topic_arn=topic_arn,
        messages=messages
    )

    # Now we update the write capacity of the SourceData table as high
    # as it can go -- we've seen issues where the table capacity fails to
    # scale up correctly, which slows down the reindexer.
    max_capacity = get_dynamodb_max_table_capacity(table_name='SourceData')
    print(f'Setting SourceData table capacity to {max_capacity}')
    set_dynamodb_table_capacity(
        table_name='SourceData',
        desired_capacity=max_capacity
    )

    for gsi_name in ('reindexTracker',):
        max_capacity = get_dynamodb_max_gsi_capacity(
            table_name='SourceData',
            gsi_name=gsi_name
        )
        print(f'Setting SourceData GSI {gsi_name} capacity to {max_capacity}')
        set_dynamodb_gsi_capacity(
            table_name='SourceData',
            gsi_name=gsi_name,
            desired_capacity=max_capacity
        )


if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        import sys
        sys.exit(1)
