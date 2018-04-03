#!/usr/bin/env python
# -*- encoding: utf-8 -*-

import collections


def sliding_window(iterable):
    """Returns a sliding window (of width 2) over data from the iterable."""
    result = collections.deque([], maxlen=2)

    for elem in iterable:
        result.append(elem)
        if len(result) == 2:
            yield tuple(list(result))
