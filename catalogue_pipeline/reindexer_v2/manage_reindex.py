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

import docopt


def create_shards(prefix, count, table_name):
    """Create new shards in the table."""


if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    default_table_name = 'AlexTest'

    if args['create-shards']:
        prefix = args['--prefix']
        count = int(args['--count'])
        table_name = args['--table'] or default_table_name

        create_shards(prefix=prefix, count=count, table_name=table_name)
