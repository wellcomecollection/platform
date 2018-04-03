#!/usr/bin/env python
# -*- encoding: utf-8 -*-

import collections
import datetime as dt

from build_windows import generate_windows


def sliding_window(iterable):
    """Returns a sliding window (of width 2) over data from the iterable."""
    result = collections.deque([], maxlen=2)

    for elem in iterable:
        result.append(elem)
        if len(result) == 2:
            yield tuple(list(result))


def get_missing_windows(report):
    """Given a report of saved Sierra windows, emit the gaps."""
    # Suppose we get two windows:
    #
    #   (-- window_1 --)
    #                       (-- window_2 --)
    #
    # We're missing any records created between the *end* of window_1
    # and the *start* of window_2, so we use these as the basis for
    # our new window.
    for window_1, window_2 in sliding_window(report):
        missing_start = window_1.end - dt.timedelta(seconds=1)
        missing_end = window_2.start + dt.timedelta(seconds=1)

        yield from generate_windows(
            start=missing_start,
            end=missing_end,
            minutes=5
        )
