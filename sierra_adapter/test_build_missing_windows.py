# -*- encoding: utf-8

from build_missing_windows import sliding_window


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
