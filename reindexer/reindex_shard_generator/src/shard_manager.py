# -*- encoding: utf-8
"""
Contains logic for building reindex shards.

A reindex shard is of the form "{source}/{id}", where "source" is the
name of the data source (e.g. "miro", "sierra"), and "id" is a shard number string
based on the ID.

The length of the shard varies based on the data source, to keep individual
reindex shards for each source at a manageable size.

"""

import math


def create_reindex_shard(source_name, source_id, source_size, shard_size):
    ascii_source_id = int(''.join(str(ord(c)) for c in source_id))

    number_of_shards = math.ceil(source_size / shard_size)

    shard_id = ascii_source_id % number_of_shards

    return f'{source_name}/{shard_id}'
