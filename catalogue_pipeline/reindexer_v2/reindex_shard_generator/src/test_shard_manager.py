# -*- encoding: utf-8

from hypothesis import assume, given
from hypothesis.strategies import integers
import pytest

from shard_manager import create_reindex_shard


@pytest.mark.parametrize(
    'source_name, source_id, source_size, expected_reindex_shard', [
    ('sierra', 'b0000001', 5e6, 'sierra/2441'),
    ('sierra', 'i0000001', 5e6, 'sierra/175'),
    ('miro', 'V0000123', 2.5e5, 'miro/9'),
])
def test_create_reindex_shard(
    source_name, source_id, source_size, expected_reindex_shard
):
    reindex_shard = create_reindex_shard(
        source_name=source_name,
        source_id=source_id,
        source_size=source_size,
        shard_size=1500
    )
    assert reindex_shard == expected_reindex_shard
