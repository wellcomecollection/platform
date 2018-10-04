# -*- encoding: utf-8

import datetime as dt

import boto3


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
