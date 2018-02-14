#!/usr/bin/env python
# -*- encoding: utf-8
"""
Create/update reindex shards in the ReindexShardTracker table.

Usage: manage_reindex.py create-shards --prefix=<PREFIX> --count=<COUNT>

Actions:
  create-shards         Create a new set of shards in the ReindexShardTracker
                        table with requestedVersion = currentVersion = 1

Options:
  --prefix=<PREFIX>     Name of the reindex shard prefix, e.g. sierra, miro
  --count=<COUNT>       How many shards to create in the table

"""
