# -*- encoding: utf-8 -*-
"""
We have a sierra_reader that reads records from Sierra, and uploads them
to files in S3.  Each file in S3 contains multiple records.

Our downstream applications want to process records one at a time, so this
demultiplexer receives the event stream of PUTs from S3, and splits each
file into individual messages on SNS.

                                S3                                  SNS

    +--------+          +-------------------+
    | reader |------>   |  r1, r2, r3, r4   |   --- demultiplexer ---+
    +--------+          +-------------------+                        |
                        |  r5, r6, r7, r8   |                        |
                        +-------------------+                        v
                        | r9, r10, r11, r12 |                      [ r1 ]
                        +-------------------+                      [ r2 ]
                                                                   [ r3 ]
                                                                    ....

"""

import json
import os

import boto3

from wellcome_aws_utils import s3_utils, sns_utils


def main(event, _ctxt=None, s3_client=None, sns_client=None):
    print(f'event = {event!r}')

    topic_arn = os.environ["TOPIC_ARN"]

    s3_client = s3_client or boto3.client('s3')
    sns_client = sns_client or boto3.client('sns')

    s3_events = s3_utils.parse_s3_record(event=event)
    assert len(s3_events) == 1
    s3_event = s3_events[0]

    resp = s3_client.get_object(
        Bucket=s3_event['bucket_name'],
        Key=s3_event['object_key']
    )
    body = resp['Body'].read()

    records = json.loads(body)
    for r in records:
        sns_utils.publish_sns_message(
            sns_client=sns_client,
            topic_arn=topic_arn,
            message=r,
            subject='source: s3_demultiplexer.main'
        )
