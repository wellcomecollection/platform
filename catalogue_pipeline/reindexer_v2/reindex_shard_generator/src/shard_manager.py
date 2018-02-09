# -*- encoding: utf-8
"""
Contains logic for building reindex shards.

A reindex shard is of the form "{source}/{id}", where "source" is the
name of the data source (e.g. "miro", "sierra"), and "id" is a hex string
based on the ID.

The length of the shard varies based on the data source, to keep individual
reindex shards for each source at a manageable size.

"""

import hashlib


def choose_shard_id_length(source_size, target_shard_size):
    """
    If there are ``source_size`` documents in this source, how many
    characters should we put in the shard ID if we want every shard to
    be no larger than ``target_shard_size``?

    Assume that shard IDs are assigned uniformly.  (May not actually be true,
    but is good enough for these purposes.)
    """
    # Shard IDs are hex strings, so each new character increases the number
    # of possible IDs by 16.
    for length in range(100):
        possible_ids = 16 ** length
        if possible_ids * target_shard_size >= source_size:
            return length

    # 100-character shard IDs allow for 2.5e120 different shards, which is
    # far more than we ever expect to need at Wellcome.  So in practice we'll
    # never hit this limit in practice; we have this branch just to avoid
    # infinite loops if something goes wrong in the above.
    raise ValueError(
        f'Unable to pick shard ID length for source_size={source_size!r} '
        f'and target_shard_size={target_shard_size!r} -- too many shards!'
    )


def create_reindex_shard(source_name, source_id, source_size):
    """Create the reindex shard for a given document."""
    shard_id_length = choose_shard_id_length(
        source_size=source_size,
        target_shard_size=1000
    )

    # We use hashing for reproducibility, and *not* for cryptographic
    # purposes.  Using MD5 or truncating your hashes are both super bad
    # if you want security!
    h = hashlib.md5()
    h.update(source_id.encode('utf8'))
    shard_id = h.hexdigest()[:shard_id_length]

    return f'{source_name}/{shard_id}'

