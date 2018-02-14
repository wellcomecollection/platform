#!/usr/bin/env python
# -*- encoding: utf-8
"""
Create/update reindex shards in the reindex shard tracker table.

Usage: manage_reindex.py update-shards --prefix=<PREFIX> --count=<COUNT> [--desired_version=<VERSION>] [--table=<TABLE>]
       manage_reindex.py -h | --help

Actions:
  update-shards         Create or update shards in the reindex tracker table.

Options:
  --prefix=<PREFIX>     Name of the reindex shard prefix, e.g. sierra, miro
  --count=<COUNT>       How many shards to create in the table
  --desired_version=<VERSION>
                        Desired version of all rows in the reindexed table
  --table=<TABLE>       Name of the reindex shard tracker DynamoDB table
  -h --help             Print this help message

"""

import time

import attr
import boto3
import docopt
from tenacity import retry, stop_after_attempt, wait_exponential


@attr.s
class Shard:
    shardId = attr.ib()
    desiredVersion = attr.ib()

    @property
    def as_dynamodb(self):
        return {
            'shardId': {'S': self.shardId},
            'desiredVersion': {'N': str(self.desiredVersion)},
        }


@retry(stop=stop_after_attempt(5), wait=wait_exponential(multiplier=1, max=10))
def _update_shard(client, table_name, shard):
    client.update_item(
        TableName=table_name,
        Key={'shardId': {'S': shard.shardId}},
        UpdateExpression=(
            'SET desiredVersion = :desiredVersion, '
            'currentVersion = if_not_exists(currentVersion, :initialCurrentVersion)'
        ),
        ExpressionAttributeValues={
            ':desiredVersion': {'N': str(shard.desiredVersion)},
            ':initialCurrentVersion': {'N': '1'},
        }
    )


def create_shards(prefix, desired_version, count, table_name):
    """Create new shards in the table."""
    new_shards = [
        Shard(shardId=f'{prefix}/{i}', desiredVersion=desired_version)
        for i in range(count)
    ]

    client = boto3.client('dynamodb')

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
    for shard in shards:
        print(f'Processing shard {shard.shardId}')
        _update_shard(client=client, table_name=table_name, shard=shard)


if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    default_table_name = 'AlexTest'

    if args['update-shards']:
        prefix = args['--prefix']
        count = int(args['--count'])
        desired_version = int(args['--desired_version'] or '1')
        table_name = args['--table'] or default_table_name

        create_shards(
            prefix=prefix,
            desired_version=desired_version,
            count=count,
            table_name=table_name
        )
