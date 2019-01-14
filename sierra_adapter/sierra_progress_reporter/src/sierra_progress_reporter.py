# -*- encoding: utf-8 -*-
"""
Query the S3 bucket containing Sierra progress reports, and log
a report in Slack
"""

import datetime as dt
import itertools
import json
import os

import boto3
import requests

from interval_arithmetic import combine_overlapping_intervals, get_intervals


def get_matching_s3_keys(s3_client, bucket, prefix):
    """
    Generate the keys in an S3 bucket that match a given prefix.
    """
    paginator = s3_client.get_paginator("list_objects")
    for page in paginator.paginate(Bucket=bucket, Prefix=prefix):
        for s3_object in page["Contents"]:
            yield s3_object["Key"]


def build_report(s3_client, bucket, resource_type):
    """
    Generate a complete set of covering windows for a resource type.
    """
    keys = get_matching_s3_keys(
        s3_client=s3_client, bucket=bucket, prefix=f"windows_{resource_type}_complete"
    )
    intervals = get_intervals(keys=keys)
    yield from combine_overlapping_intervals(intervals)


def chunks(iterable, chunk_size):
    return (iterable[i : i + chunk_size] for i in range(0, len(iterable), chunk_size))


def get_consolidated_report(s3_client, bucket, resource_type):
    report = build_report(
        s3_client=s3_client, bucket=bucket, resource_type=resource_type
    )

    for iv, running in report:
        if len(running) > 1:
            # Create a consolidated marker that represents the entire
            # interval.  The back-history of Sierra includes >100k windows,
            # so combining them makes reporting faster on subsequent runs.
            start_str = iv.start.strftime("%Y-%m-%dT%H-%M-%S.%f+00-00")
            end_str = iv.end.strftime("%Y-%m-%dT%H-%M-%S.%f+00-00")

            consolidated_key = (
                f"windows_{resource_type}_complete/{start_str}__{end_str}"
            )

            s3_client.put_object(Bucket=bucket, Key=consolidated_key, Body=b"")

            # Then clean up the individual intervals that made up the set.
            # We sacrifice granularity for performance.
            for sub_ivs in chunks(running, chunk_size=1000):
                keys = [s.key for s in sub_ivs if s.key != consolidated_key]
                s3_client.delete_objects(
                    Bucket=bucket, Delete={"Objects": [{"Key": k} for k in keys]}
                )

        yield iv


class IncompleteReportError(Exception):
    pass


def process_report(s3_client, bucket, resource_type):
    for iv in get_consolidated_report(s3_client, bucket, resource_type):

        # If the first gap is more than 6 hours old, we might have a
        # bug in the Sierra reader.  Raise an exception.
        hours = (dt.datetime.now() - iv.end).total_seconds() / 3600
        if hours > 6:
            raise IncompleteReportError(resource_type)


def prepare_present_report(s3_client, bucket, resource_type):
    """
    Generate a report for windows that are present.
    """
    yield ""
    yield f"*{resource_type} windows*"

    for iv in get_consolidated_report(s3_client, bucket, resource_type):
        yield f"{iv.start.isoformat()} – {iv.end.isoformat()}"

    yield ""


# https://stackoverflow.com/q/6822725/1558022
def window(seq, n=2):
    """
    Returns a sliding window (of width n) over data from the iterable

        s -> (s0,s1,...s[n-1]), (s1,s2,...,sn),

    """
    it = iter(seq)
    result = tuple(itertools.islice(it, n))
    if len(result) == n:
        yield result
    for elem in it:
        result = result[1:] + (elem,)
        yield result


def prepare_missing_report(s3_client, bucket, resource_type):
    """
    Generate a report for windows that are missing.
    """
    yield ""
    yield f"*missing {resource_type} windows*"

    for iv1, iv2 in window(get_consolidated_report(s3_client, bucket, resource_type)):
        missing_start = iv1.end
        missing_end = iv2.start
        if missing_start.date() == missing_end.date():
            yield f"{missing_start.date()}: {missing_start.strftime('%H:%M:%S')} — {missing_end.strftime('%H:%M:%S')}"
        else:
            yield f"{missing_start.strftime('%Y-%m-%d %H:%M:%S')} — {missing_end.strftime('%Y-%m-%d %H:%M:%S')}"

    yield ""


def print_report(s3_client, bucket, resource_type):
    print("\n".join(prepare_present_report(s3_client, bucket, resource_type)))


def main(event=None, _ctxt=None):
    s3_client = boto3.client("s3")
    bucket = os.environ["BUCKET"]
    slack_webhook = os.environ["SLACK_WEBHOOK"]

    errors = []
    error_lines = []

    for resource_type in ("bibs", "items"):
        try:
            process_report(
                s3_client=s3_client, bucket=bucket, resource_type=resource_type
            )
        except IncompleteReportError:
            error_lines.extend(
                prepare_missing_report(
                    s3_client=s3_client, bucket=bucket, resource_type=resource_type
                )
            )
            errors.append(resource_type)

    if errors:
        if errors == ["bibs"]:
            message = "There are gaps in the bib data."
        elif errors == ["items"]:
            message = "There are gaps in the item data."
        else:
            message = "There are gaps in the bib and the item data."

        error_lines.insert(0, message)

        error_lines.append(
            "You can fix this by running `$ python sierra_adapter/send_missing_windows.py` in the root of the platform repo."
        )

        slack_data = {
            "username": "sierra-reader",
            "icon_emoji": ":sierra:",
            "attachments": [
                {
                    "color": "#8B4F30",
                    "text": "\n".join(error_lines).strip(),
                    "mrkdwn_in": ["text"],
                }
            ],
        }

        resp = requests.post(
            slack_webhook,
            data=json.dumps(slack_data),
            headers={"Content-Type": "application/json"},
        )
        resp.raise_for_status()


if __name__ == "__main__":
    s3_client = boto3.client("s3")
    bucket = "wellcomecollection-platform-adapters-sierra"

    for resource_type in ("bibs", "items"):
        print_report(s3_client=s3_client, bucket=bucket, resource_type=resource_type)
