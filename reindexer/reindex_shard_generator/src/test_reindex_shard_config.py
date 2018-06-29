# -*- encoding: utf-8

import pytest

from reindex_shard_config import create_reindex_shard


@pytest.mark.parametrize(
    'source_name, id, expected_reindex_shard', [
    ('sierra', 'b0000001', 'sierra/2441'),
])
def test_create_reindex_shard(source_name, source_id, expected_reindex_shard):
    reindex_shard = create_reindex_shard(
        source_name=source_name,
        source_id=source_id
    )
    assert reindex_shard == expected_reindex_shard
