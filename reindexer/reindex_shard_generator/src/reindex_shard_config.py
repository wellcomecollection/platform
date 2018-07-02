# -*- encoding: utf-8
"""
Contains shared config for managing reindex shards.

This code is used by:

-   The reindex shard generator
-   The trigger reindex script

"""

import math


# How many records are there in each source?
SOURCE_SIZES = {
    'sierra': 5e6,
    'miro': 2.5e5,
}

# What's the average number of records per shard that we're aiming for?
SHARD_SIZE = 1500


def get_number_of_shards(source_name):
    """How many shards should we create for a given source?"""
    try:
        return math.ceil(SOURCE_SIZES[source_name] / SHARD_SIZE)
    except KeyError:
        raise ValueError(
            f'Unrecognised source name {source_name!r}; '
            f'expected {list(SOURCE_SIZES.keys())}'
        )


def create_reindex_shard(source_name, source_id):
    """What reindex shard should a given source name/ID pair be placed in?"""

    number_of_shards = get_number_of_shards(source_name)

    # This line gets the ASCII code point value of each character in
    # the source ID, and adds them together.  e.g. a => 97, b => 98, c => 99
    #
    # The exact algorithm isn't especially important -- our goal is to
    # pick values that get an even spread.  Since source IDs are sequential,
    # this approach should ensure that adjacent IDs end up in different
    # reindex shards (which should spread the load on S3 when we reindex
    # a VHS-backed table).
    #
    ascii_id = int(''.join(str(ord(c)) for c in source_id))

    shard_id = ascii_id % number_of_shards

    return f'{source_name}/{shard_id}'
