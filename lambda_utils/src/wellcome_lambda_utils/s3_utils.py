# -*- encoding: utf-8 -*-

import dateutil.parser

import boto3
from botocore.exceptions import ClientError


def is_object(bucket, key):
    """
    Checks if an object exists in S3.  Returns True/False.
    """
    client = boto3.client('s3')
    try:
        client.head_object(Bucket=bucket, Key=key)
    except ClientError as err:
        if err.response['Error']['Code'] == '404':
            return False
        else:
            raise
    else:
        return True


def copy_object(src_bucket, src_key, dst_bucket, dst_key, lazy=False):
    """
    Copy an object from one S3 bucket to another.

    If you pass ``lazy=True``, the object will only be copied if the
    destination object either:
     * does not exist, or
     * does exist, but has the same ETag as the source object

    """
    client = boto3.client('s3')
    if not is_object(bucket=src_bucket, key=src_key):
        raise ValueError(
            f'Tried to copy missing object ({src_bucket}, {src_key})'
        )

    def should_copy():
        if not lazy:
            return True

        if not is_object(bucket=dst_bucket, key=dst_key):
            return True

        src_resp = client.head_object(Bucket=src_bucket, Key=src_key)
        dst_resp = client.head_object(Bucket=dst_bucket, Key=dst_key)
        return src_resp['ETag'] == dst_resp['ETag']

    if should_copy():
        return client.copy_object(
            CopySource={'Bucket': src_bucket, 'Key': src_key},
            Bucket=dst_bucket,
            Key=dst_key
        )


def _extract_s3_event(record):
    event_datetime = dateutil.parser.parse(record["eventTime"])

    return {
        "event_name": record["eventName"],
        "event_time": event_datetime,
        "bucket_name": record["s3"]["bucket"]["name"],
        "object_key": record["s3"]["object"]["key"],
        "size": record["s3"]["object"]["size"],
        "versionId": record["s3"]["object"].get("versionId")
    }


def parse_s3_record(event):
    """
    Extracts a simple subset of an S3 update event.
    """
    return [_extract_s3_event(record) for record in event["Records"]]