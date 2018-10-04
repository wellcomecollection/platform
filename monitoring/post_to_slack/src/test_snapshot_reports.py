# -*- encoding: utf-8

import pytest

from snapshot_reports import pprint_timedelta


@pytest.mark.parametrize(
    "seconds, expected_string",
    [
        (1, "1s"),
        (15, "15s"),
        (60, "1m"),
        (67, "1m 7s"),
        (3600, "1h"),
        (3660, "1h 1m"),
        (3661, "1h 1m"),
        (86400, "1d"),
        (90000, "1d 1h"),
        (90010, "1d 1h"),
    ],
)
def test_pprint_timedelta(seconds, expected_string):
    assert pprint_timedelta(seconds) == expected_string
