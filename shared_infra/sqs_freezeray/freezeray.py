#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Save all the messages from an SQS queue to a file in S3.

Usage: freezeray.py --src=<SRC_QUEUE_URL> --bucket=<S3_BUCKET>
       freezeray.py -h | --help

"""

import datetime as dt
import os
import logging

import daiquiri
import docopt
from wellcome_aws_utils import s3_utils, sqs_utils


daiquiri.setup(level=logging.INFO)

logger = daiquiri.getLogger(__name__)


def write_all_messages_to_s3(bucket, key, src_queue_url):
    """
    Write all the messages from a queue to an S3 bucket.
    """
    messages = []

    def write():
        logger.info(
            'Writing %d messages to s3://%s/%s',
            len(messages), bucket, key)
        s3_utils.write_dicts_to_s3(bucket=bucket, key=key, dicts=messages)

    generator = sqs_utils.get_messages(
        src_queue_url=src_queue_url, delete=True
    )
    for i, message in enumerate(generator):
        messages.append(message)

        # Because messages are deleted after they're processed, and received
        # in batches of 10, we write to S3 after every 10 messages.  If the
        # task exits halfway through, we haven't lost data.
        if i % 10 == 0:
            write()

    write()


if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    src_queue_url = args['--src']
    bucket = args['--bucket']

    queue_name = os.path.basename(queue_url)

    date_string = dt.datetime.now().strftime('%Y-%m-%d_%H-%M-%S')
    key = f'sqs/{queue_name}_{date_string}.txt'

    write_all_messages_to_s3(
        bucket=bucket, key=key, src_queue_url=src_queue_url
    )
