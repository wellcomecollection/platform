# -*- encoding: utf-8

from hypothesis import assume, given
from hypothesis.strategies import integers
import pytest

from shard_manager import choose_shard_id_length


@pytest.mark.parametrize(
        'source_size, target_shard_size, expected_shard_length', [
    (100, 1, 2),
    (100, 50, 1),
    (100, 100, 0),
])
def test_example_shard_lengths(
    source_size, target_shard_size, expected_shard_length
):
    shard_length = choose_shard_id_length(
        source_size=source_size,
        target_shard_size=target_shard_size
    )
    assert shard_length == expected_shard_length


def test_too_many_shards_is_valueerror():
    with pytest.raises(ValueError):
        choose_shard_id_length(source_size=3e150, target_shard_size=1)


@given(
    source_size=integers(min_value=1),
    target_shard_size=integers(min_value=1))
def test_shard_sizes_are_plausible(source_size, target_shard_size):
    try:
        shard_length = choose_shard_id_length(
            source_size=source_size,
            target_shard_size=target_shard_size
        )
    except ValueError:
        assume(False)
    else:
        assert source_size <= (16 ** shard_length) * target_shard_size
