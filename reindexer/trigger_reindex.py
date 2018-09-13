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


def post_to_slack(source_name, reason, total_segments):
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
        f'Reason: *{reason}* '
        f'({total_segments} segment{"s" if total_segments != 1 else ""})'
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


def check_tables_are_clear():
    """
    Check that all the tables in the pipeline are clear before starting
    a reindex.
    """
    dynamodb = boto3.client('dynamodb')

    table_names = dynamodb.list_tables()['TableNames']

    for table_suffix in [
        'matcher-lock-table',
        'works-graph',
        'Recorder',
    ]:
        matching_tables = [t for t in table_names if t.endswith(table_suffix)]
        if len(matching_tables) == 0:
            print(f'Unable to find a pipeline table for {table_suffix!r}?')
            sys.exit(1)
        elif len(matching_tables) > 1:
            print(f'More than one pipeline table for {table_suffix!r}: {matching_tables}')
            sys.exit(1)

        matching_table = matching_tables[0]

        resp = dynamodb.scan(TableName=matching_table, Limit=1)
        if resp['Items']:
            print(f'Table {matching_table!r} is already populated!  Clear tables before reindexing.')
            sys.exit(1)


def main():
    args = docopt.docopt(__doc__)

    source_name = args['--source']
    reason = args['--reason']
    total_segments = int(args['--total_segments'])

    print('Checking pipeline is clear...')
    check_tables_are_clear()

    print(f'Triggering a reindex in {source_name}')

    post_to_slack(
        source_name=source_name,
        reason=reason,
        total_segments=total_segments
    )

    messages = all_messages(total_segments=total_segments)

    topic_arn = build_topic_arn(topic_name=get_topic_name(source_name))

    publish_messages(
        topic_arn=topic_arn,
        messages=messages,
        total_segments=total_segments
    )


if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        sys.exit(1)
