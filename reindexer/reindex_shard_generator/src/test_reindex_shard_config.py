# -*- encoding: utf-8

import pytest

from reindex_shard_config import create_reindex_shard


@pytest.mark.parametrize(
    'source_name, source_id, expected_reindex_shard', [
    ('sierra', 'b0000001', 'sierra/2441'),
    ('miro', 'A0000001', 'miro/128')
])
def test_create_reindex_shard(source_name, source_id, expected_reindex_shard):
    reindex_shard = create_reindex_shard(
        source_name=source_name,
        source_id=source_id
    )
    assert reindex_shard == expected_reindex_shard


@pytest.mark.parametrize('source_name', ['foo', 13, None])
def test_unrecognised_source_name_is_ValueError(source_name):
    with pytest.raises(ValueError) as err:
        create_reindex_shard(
            source_name=source_name,
            source_id='0001'
        )
    assert err.value.args[0].startswith('Unrecognised source name')
    assert repr(source_name) in err.value.args[0]
