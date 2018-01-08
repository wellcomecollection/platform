#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Copy all the messages from one SQS queue to another.

Usage: redrive.py --src=<SRC_QUEUE_URL> --dst=<DST_QUEUE_URL>
       redrive.py -h | --help

"""

import logging

import boto3
import daiquiri
import docopt
from wellcome_aws_utils import sqs_utils


daiquiri.setup(level=logging.INFO)
logger = daiquiri.getLogger(__name__)


def redrive(src_queue_url, dst_queue_url):
    """
    Republish all the messages on ``src_queue_url`` into ``dst_queue_url``.
    """
    client = boto3.client('sqs')
    messages = sqs_utils.get_messages(queue_url=src_queue_url, delete=True)

    for msg in messages:
        client.send_message(
            QueueUrl=dst_queue_url,
            MessageBody=msg['Body']
        )


if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    src_queue_url = args['--src']
    dst_queue_url = args['--dst']

    logging.info("Source queue: %s", src_queue_url)
    logging.info("Dest queue:   %s", dst_queue_url)

    redrive(src_queue_url=src_queue_url, dst_queue_url=dst_queue_url)
    logging.info("Done")
