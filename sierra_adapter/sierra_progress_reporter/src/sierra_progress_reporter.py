# -*- encoding: utf-8 -*-
"""
Query the S3 bucket containing Sierra progress reports, and log
a report in Slack
"""

import boto3


def get_matching_s3_keys(s3_client, bucket, prefix):
    """
    Generate the keys in an S3 bucket that match a given prefix.
    """
    paginator = s3_client.get_paginator('list_objects')
    for page in paginator.paginate(Bucket=bucket, Prefix=prefix):
        for s3_object in page['Contents']:
            yield s3_object['Key']


def main(event=None, _ctxt=None):
    print('Hello world!')
