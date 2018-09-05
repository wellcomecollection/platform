#!/usr/bin/env python
# -*- encoding: utf-8
"""
Create/update reindex shards in the reindex shard tracker table.

Usage: trigger_reindex.py --source=<SOURCE_NAME> --reason=<REASON> --total_segments=<COUNT>
       trigger_reindex.py -h | --help

Options:
  --source=<SOURCE_NAME>    Name of the source you want to reindex.
  --reason=<REASON>         An explanation of why you're running this reindex.
                            This will be printed in the Slack alert.
  --total_segments=<COUNT>  How many segments to divide the VHS table into.
  -h --help                 Print this help message

"""

import json
import sys

import boto3
import docopt
import hcl
import requests
import tqdm

from dynamodb_capacity_helpers import (
    get_dynamodb_max_table_capacity,
    set_dynamodb_table_capacity
)


DYNAMO_CONFIGS = {
    'miro': {'table': 'vhs-sourcedata-miro'},
    'sierra': {'table': 'vhs-sourcedata-sierra'},
}


def get_topic_name(source_name):
    return f'reindex_jobs-{source_name}'


def all_messages(total_segments):
    """
    Generates all the messages to be sent to SNS.
    """
    for i in range(total_segments):
        yield {
            'segment': i,
            'totalSegments': total_segments
        }


def publish_messages(topic_arn, messages, total_segments):
    """Publish a sequence of messages to an SNS topic."""
    sns_client = boto3.client('sns')
    for m in tqdm.tqdm(messages, total=total_segments):
        resp = sns_client.publish(
            TopicArn=topic_arn,
            MessageStructure='json',
            Message=json.dumps({
                'default': json.dumps(m)
            }),
            Subject=f'Source: {__file__}'
        )
        assert resp['ResponseMetadata']['HTTPStatusCode'] == 200, resp


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

    message = (
        f'*{username}* started a reindex in *{source_name}*\n'
        f'Reason: *{reason}*'
    )

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
    total_segments = int(args['--total_segments'])

    print(f'Triggering a reindex in {source_name}')

    post_to_slack(source_name=source_name, reason=reason)

    messages = all_messages(total_segments=total_segments)

    topic_arn = build_topic_arn(topic_name=get_topic_name(source_name))

    publish_messages(
        topic_arn=topic_arn,
        messages=messages,
        total_segments=total_segments
    )

    # Now we update the write capacity of the SourceData table as high
    # as it can go -- we've seen issues where the table capacity fails to
    # scale up correctly, which slows down the reindexer.
    dynamo_config = DYNAMO_CONFIGS[source_name]
    max_capacity = get_dynamodb_max_table_capacity(
        table_name=dynamo_config['table']
    )

    table_name = dynamo_config['table']

    print(f'Setting {table_name} table capacity to {max_capacity}')
    set_dynamodb_table_capacity(
        table_name=table_name,
        desired_capacity=max_capacity
    )


if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        sys.exit(1)
