# -*- encoding: utf-8

import datetime as dt

from build_missing_windows import get_missing_windows, sliding_window
from report_adapter_progress import Interval


def test_sliding_window():
    res = sliding_window(range(10))
    assert list(res) == [
        (0, 1),
        (1, 2),
        (2, 3),
        (3, 4),
        (4, 5),
        (5, 6),
        (6, 7),
        (7, 8),
        (8, 9),
    ]


def test_missing_windows():
    report = [
        Interval(
            start = dt.datetime(2000, 1, 1, 12, 0, 0),   # noqa
            end   = dt.datetime(2000, 1, 1, 13, 0, 0),   # noqa
            key='report1.txt'
        ),
        Interval(
            start = dt.datetime(2000, 1, 1, 13, 7, 0),   # noqa
            end   = dt.datetime(2000, 1, 1, 13, 18, 0),  # noqa
            key='report2.txt'
        ),
        Interval(
            start = dt.datetime(2000, 1, 1, 13, 25, 0),  # noqa
            end   = dt.datetime(2000, 1, 1, 13, 40, 0),  # noqa
            key='report3.txt'
        ),
    ]  # noqa

    res = get_missing_windows(report)
    assert list(res) == [
        {
            'start': dt.datetime(2000, 1, 1, 12, 59, 59).isoformat(),
            'end':   dt.datetime(2000, 1, 1, 13, 4, 59).isoformat(),  # noqa
        },
        {
            'start': dt.datetime(2000, 1, 1, 13, 3, 59).isoformat(),
            'end':   dt.datetime(2000, 1, 1, 13, 8, 59).isoformat(),  # noqa
        },
        {
            'start': dt.datetime(2000, 1, 1, 13, 17, 59).isoformat(),
            'end':   dt.datetime(2000, 1, 1, 13, 22, 59).isoformat(),  # noqa
        },
        {
            'start': dt.datetime(2000, 1, 1, 13, 21, 59).isoformat(),
            'end':   dt.datetime(2000, 1, 1, 13, 26, 59).isoformat(),  # noqa
        },
    ]
