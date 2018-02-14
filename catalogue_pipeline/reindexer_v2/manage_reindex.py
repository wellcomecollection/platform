#!/usr/bin/env python
# -*- encoding: utf-8
"""
Create/update reindex shards in the reindex shard tracker table.

Usage: manage_reindex.py create-shards --prefix=<PREFIX> --count=<COUNT> [--table=<TABLE>]

Actions:
  create-shards         Create a new set of shards in the reindex shard tracker
                        table with requestedVersion = currentVersion = 1

Options:
  --prefix=<PREFIX>     Name of the reindex shard prefix, e.g. sierra, miro
  --count=<COUNT>       How many shards to create in the table
  --table=<TABLE>       Name of the reindex shard tracker DynamoDB table

"""

import time

import attr
import boto3
import docopt


@attr.s
class Shard:
    shardId = attr.ib()
    currentVersion = attr.ib()
    desiredVersion = attr.ib()

    @property
    def as_dynamodb(self):
        return {
            'shardId': {'S': self.shardId},
            'currentVersion': {'N': str(self.currentVersion)},
            'desiredVersion': {'N': str(self.desiredVersion)},
        }


def _batch_write(shards, table_name):
    """
    Given a list of ``Shard`` instances, write them all into ``table_name``.
    """
    dynamodb_client = boto3.client('dynamodb')

    # This is a slightly idiomatic way to run the loop; the reason is that
    # ``batch_write_item`` may sometimes fail to PUT an item, in which case we
    # want to retry it.  So we whittle down this list until everything
    # PUTs successfully.
    while shards:

        # We can send up to 25 items in a single ``batch_write_item`` request.
        next_batch = shards[:25]

        put_requests = [
            {'PutRequest': {'Item': shard.as_dynamodb}} for shard in next_batch
        ]

        resp = dynamodb_client.batch_write_item(
            RequestItems={table_name: put_requests}
        )

        # If an item fails to PUT correctly, it appears in the
        # "UnprocessedItems" field of the response.  If so, we send it back
        # around for processing again.  Otherwise, we can delete it.
        # from pprint import pprint
        # pprint(resp['UnprocessedItems'])
        try:
            missing_items = [
                request['PutRequest']['Item']
                for request in resp['UnprocessedItems'][table_name]
            ]
        except KeyError:
            missing_items = []

        for item in next_batch:
            if item.as_dynamodb in missing_items:
                continue
            else:
                shards.remove(item)

        print(f'{len(next_batch) - len(missing_items)} shards placed successfully!')

        # Ideally this would be an exponential backoff, but it's not worth
        # writing that complexity for a script that will be used infrequently.
        if missing_items:
            print(f'{len(missing_items)} failed to process, sleeping briefly...')
            time.sleep(1)


def create_shards(prefix, count, table_name):
    """Create new shards in the table."""
    new_shards = [
        Shard(shardId=f'{prefix}/{i}', currentVersion=1, desiredVersion=1)
        for i in range(count)
    ]

    _batch_write(shards=new_shards, table_name=table_name)


if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    default_table_name = 'AlexTest'

    if args['create-shards']:
        prefix = args['--prefix']
        count = int(args['--count'])
        table_name = args['--table'] or default_table_name

        create_shards(prefix=prefix, count=count, table_name=table_name)
