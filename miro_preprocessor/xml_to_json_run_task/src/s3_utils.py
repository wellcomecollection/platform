# -*- encoding: utf-8 -*-

import dateutil.parser


def _extract_s3_event(record):
    event_datetime = dateutil.parser.parse(record["eventTime"])

    return {
        "event_name": record["eventName"],
        "event_time": event_datetime,
        "bucket_name": record["s3"]["bucket"]["name"],
        "object_key": record["s3"]["object"]["key"],
        "size": record["s3"]["object"]["size"],
        "versionId": record["s3"]["object"]["versionId"]
    }


def parse_s3_record(event):
    """
    Extracts a simple subset of an S3 update event.
    """
    return [_extract_s3_event(record) for record in event["Records"]]
