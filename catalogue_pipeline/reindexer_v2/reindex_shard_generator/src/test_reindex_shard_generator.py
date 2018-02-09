# -*- encoding: utf-8

from hypothesis import given
from hypothesis.strategies import builds, iterables, just, one_of, text

from reindex_shard_generator import filter_rows_that_need_shard


@given(rows=iterables(
    builds(
        dict,
        id=text(min_size=1),
        reindexShard=one_of(just('default'), text())
    )
))
def test_filter_rows_that_need_shard(rows):
    for r in filter_rows_that_need_shard(rows):
        assert r['reindexShard'] == 'default'
