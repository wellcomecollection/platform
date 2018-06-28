#!/usr/bin/env python
# -*- encoding: utf-8
"""
Create/update reindex shards in the reindex shard tracker table.

Usage: trigger_reindex.py --prefix=<PREFIX> --reason=<REASON> [--count=<COUNT>]
       trigger_reindex.py -h | --help

Options:
  --prefix=<PREFIX>     Name of the reindex shard prefix, e.g. sierra, miro
  --reason=<REASON>     An explanation of why you're running this reindex.
                        This will be printed in the Slack alert.
  --count=<COUNT>       How many shards to create in the table
  -h --help             Print this help message

"""

import datetime as dt
import json

import boto3
import docopt
import hcl
import requests
import tqdm


TABLE_NAME = 'ReindexShardTracker'


# Implementation note: this code may fail if you hit the write limit on
# DynamoDB, and interrupt midway through updating a prefix.
#
#  1. Decorating this function with the tenacity library
#     (http://tenacity.readthedocs.io/en/latest/) should make it more likely
#     to succeed, as it will retry in case of error.
#
#  2. The whole process is idempotent, so it's enough to bump the capacity
#     and re-run the prefix.
#
# FWIW, I was able to create >5000 shards with WriteCapacity=1 and autoscaling
# on a test table, so this may be a theoretical concern!

def _update_shard(client, table_name, shard):
    client.update_item(
        TableName=table_name,
        Key={'shardId': {'S': shard['shardId']}},

        # We want to update the desiredVersion of every row in the table, and
        # set the currentVersion to 1, but *only* if there isn't already an
        # existing value of currentVersion in the table.
        UpdateExpression=(
            'SET desiredVersion = :desiredVersion, '
            'currentVersion = if_not_exists(currentVersion, :initialCurrentVersion)'
        ),
        ExpressionAttributeValues={
            ':desiredVersion': {'N': str(shard['desiredVersion'])},
            ':initialCurrentVersion': {'N': '1'},
        }
    )


def _all_records_in_shard(client, table_name):
    """Generates every row in a particular shard."""
    paginator = client.get_paginator('scan')
    for page in paginator.paginate(TableName=table_name):
        for i in page['Items']:
            yield i


def _all_shard_names(client, table_name):
    """Generates the name of all current shards."""
    for shard in _all_records_in_shard(client=client, table_name=table_name):
        yield shard['shardId']['S']


def _count_current_shards(client, prefix, table_name):
    """How many shards are there in the current table?"""
    best_seen = None
    for s in _all_shard_names(client=client, table_name=table_name):
        if s.startswith(prefix):
            value = int(s.replace(prefix, '').strip('/'))
            if (best_seen is None) or (value > best_seen):
                best_seen = value

    return best_seen


def create_shards(client, prefix, desired_version, count, table_name):
    """Create new shards in the table."""
    new_shards = [
        {'shardId': f'{prefix}/{i}', 'desiredVersion': desired_version}
        for i in range(count)
    ]

    # Implementation note: this is potentially quite slow, as we make a new
    # UPDATE request for every shard we want to write to DynamoDB.  The
    # reason is that if we just PUT an item, we delete whatever's already
    # there -- potentially including the currentVersion.
    #
    # We could speed this up slightly by:
    #
    #   1.  GET the current row (if any)
    #   2.  Construct the new row, and try to PUT that, using a conditional
    #       update to check nobody has updated :currentVersion in the meantime
    #   3.  Repeat 1-2 until we get an update that succeeds
    #
    # That would be significantly more complicated to implement, so for now
    # we choose simplicity over speed.
    #
    # If you're finding this script to be too slow, this is where to start.
    for shard in tqdm.tqdm(new_shards):
        _update_shard(client=client, table_name=table_name, shard=shard)


def post_to_slack(prefix, reason):
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

    slack_data = {
        'username': 'reindex-tracker',
        'icon_emoji': ':dynamodb:',
        'color': '#2E72B8',
        'title': 'reindexer',
        'fields': [{
            'value': '*%s* started a reindex in *%s*\nReason: *%s*' % (
                username, prefix, reason
            )
        }]
    }

    resp = requests.post(
        webhook_url,
        data=json.dumps(slack_data),
        headers={'Content-Type': 'application/json'}
    )
    resp.raise_for_status()


if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    client = boto3.client('dynamodb')

    prefix = args['--prefix']
    count = int(args['--count'] or '0')
    reason = args['--reason']
    table_name = TABLE_NAME

    # We use the current timestamp for the reindex version -- this allows
    # us to easily trace when a reindex was triggered.
    desired_version = int(dt.datetime.utcnow().timestamp())
    print(f'Updating all shards in {prefix} to {desired_version}')

    post_to_slack(prefix=prefix, reason=reason)

    if count == 0:
        count = _count_current_shards(
            client=client,
            table_name=table_name,
            prefix=prefix
        )

    create_shards(
        client=client,
        prefix=prefix,
        desired_version=desired_version,
        count=count,
        table_name=table_name
    )
