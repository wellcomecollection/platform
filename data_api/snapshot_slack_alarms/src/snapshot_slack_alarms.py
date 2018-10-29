# -*- encoding: utf-8

import datetime as dt
import json
import os
import re

import boto3
from botocore.vendored import requests


# An alarm message is typically of the form:
#
#   Threshold Crossed: 1 datapoint [2.0 (05/02/18 06:35:00)] was greater than
#   or equal to the threshold (1.0).
#
THRESHOLD_RE = re.compile(
    r"^Threshold Crossed: "
    r"1 datapoint "
    r"\[(?P<actual_value>\d+)\.0 \(\d{2}/\d{2}/\d{2} \d{2}:\d{2}:\d{2}\)\] "
    r"was "
    r"(?:greater|less) than(?: or equal to)? "
    r"the threshold \(\d+\.\d+\)."
)


def pprint_timedelta(seconds):
    """
    Returns a pretty-printed summary of a duration as seconds.

    e.g. "1h", "2d 3h", "1m 4s".

    """
    days, seconds = divmod(seconds, 86400)
    hours, seconds = divmod(seconds, 3600)
    minutes, seconds = divmod(seconds, 60)

    if days > 0:
        if hours == 0:
            return "%dd" % days
        else:
            return "%dd %dh" % (days, hours)

    elif hours > 0:
        if minutes == 0:
            return "%dh" % hours
        else:
            return "%dh %dm" % (hours, minutes)

    elif minutes > 0:
        if seconds == 0:
            return "%dm" % minutes
        else:
            return "%dm %ds" % (minutes, seconds)
    else:
        return "%ds" % seconds


def get_snapshot_report():
    """
    Try to return a string that describes the latest snapshots.
    """
    lines = []
    now = dt.datetime.now()

    s3 = boto3.client("s3")

    for version in ["v1", "v2"]:
        try:
            # Yes, this makes a bunch of hard-coded assumptions about the
            # way the bucket is laid out.  It's a quick-win helper for
            # bug diagnosis, not a prod API.
            s3_object = s3.head_object(
                Bucket="wellcomecollection-data-public",
                Key=f"catalogue/{version}/works.json.gz",
            )

            last_modified_date = s3_object["LastModified"].replace(tzinfo=None)
            seconds = (now - last_modified_date).seconds
            lines.append(
                f"{version}: {pprint_timedelta(seconds)} ago ({last_modified_date.isoformat()})"
            )
        except Exception:
            pass

    return "\n".join(lines)


def prepare_slack_payload(alarm_name, state_reason, latest_snapshots_str):
    """
    Prepare the payload to send to Slack.
    """
    match = THRESHOLD_RE.match(state_reason)
    assert match is not None, state_reason

    error_count = match.group("actual_value")
    if error_count == 1:
        message = "The snapshot generator queue has 1 unprocessed item."
    else:
        message = f"The snapshot generator queue has {error_count} unprocessed items."

    slack_data = {
        "username": "snapshot-alarm",
        "icon_emoji": ":rotating_light:",
        "attachments": [
            {
                "color": "danger",
                "fallback": alarm_name,
                "title": alarm_name,
                "fields": [{"value": message}],
            }
        ],
    }

    if latest_snapshots_str is not None:
        slack_data["attachments"][0]["fields"].append(
            {"title": "Latest snapshots", "value": latest_snapshots_str}
        )

    return slack_data


def main(event, _ctxt=None):
    webhook_url = os.environ["CRITICAL_SLACK_WEBHOOK"]

    latest_snapshots_str = get_snapshot_report()

    for record in event["Records"]:
        message = json.loads(record["Sns"]["Message"])

        alarm_name = message["AlarmName"]
        state_reason = message["NewStateReason"]

        slack_data = prepare_slack_payload(
            alarm_name=alarm_name,
            state_reason=state_reason,
            latest_snapshots_str=latest_snapshots_str,
        )

        resp = requests.post(
            webhook_url,
            data=json.dumps(slack_data),
            headers={"Content-Type": "application/json"},
        )
        resp.raise_for_status()
