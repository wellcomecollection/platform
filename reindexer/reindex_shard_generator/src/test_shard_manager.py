# -*- encoding: utf-8

import pytest

from shard_manager import create_reindex_shard


@pytest.mark.parametrize(
    'source_name, id, source_size, shard_size, expected_reindex_shard', [
    ('sierra', 'b0000001', 5e6, 1500, 'sierra/2441'),
    ('sierra', 'i0000001', 5e6, 2000, 'sierra/2349'),
    ('miro', 'V0000123', 2.5e5, 1000, 'miro/51'),
])
def test_create_reindex_shard(
    source_name, id, source_size, shard_size, expected_reindex_shard
):
    reindex_shard = create_reindex_shard(
        source_name=source_name,
        id=id,
        source_size=source_size,
        shard_size=shard_size
    )
    assert reindex_shard == expected_reindex_shard
