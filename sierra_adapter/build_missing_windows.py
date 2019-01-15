#!/usr/bin/env python
# -*- encoding: utf-8 -*-

import collections
import datetime as dt
import json
import os
import sys

import boto3

sys.path.append(
    os.path.join(os.path.dirname(__file__), "sierra_progress_reporter", "src")
)
sys.path.append(
    os.path.join(os.path.dirname(__file__), "sierra_window_generator", "src")
)

from build_windows import generate_windows  # noqa
from sierra_progress_reporter import build_report  # noqa


BUCKET = "wellcomecollection-platform-adapters-sierra"


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
    for (window_1, _), (window_2, _) in sliding_window(report):
        missing_start = window_1.end - dt.timedelta(seconds=1)
        missing_end = window_2.start + dt.timedelta(seconds=1)

        yield from generate_windows(start=missing_start, end=missing_end, minutes=2)


if __name__ == "__main__":
    client = boto3.client("sns")

    for resource_type in ("bibs", "items"):
        report = build_report(
            s3_client=boto3.client("s3"), bucket=BUCKET, resource_type=resource_type
        )
        for missing_window in get_missing_windows(report):
            print(missing_window)
            client.publish(
                TopicArn=f"arn:aws:sns:eu-west-1:760097843905:sierra_{resource_type}_windows",
                Message=json.dumps(missing_window),
                Subject=f"Window sent by {__file__}",
            )
