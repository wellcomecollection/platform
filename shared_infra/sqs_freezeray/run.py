#!/usr/bin/env python
# -*- encoding: utf-8 -*-

import datetime as dt
import os
import logging

import boto3
import daiquiri
from wellcome_lambda_utils import s3_utils


daiquiri.setup(level=logging.INFO)

logger = daiquiri.getLogger(__name__)


def get_messages(queue_url, burn_after_reading=False, batch_size=10):
    """
    Gets messages from an SQS queue.  If ``burn_after_reading`` is True, the
    messages are also deleted after they've been read.
    """
    client = boto3.client('sqs')
    while True:
        # We batch message responses to reduce load on the SQS API.
        # Note: 10 is currently the most messages you can read at once.
        resp = client.receive_message(
            QueueUrl=queue_url,
            AttributeNames=['All'],
            MaxNumberOfMessages=batch_size
        )

        # If there's nothing available, the queue is empty.  Abort!
        try:
            logger.info(
                'Received %d new messages from %s',
                len(resp['Messages']), queue_url)
        except KeyError:
            logger.info('No messages received from %s; aborting', queue_url)
            break

        # If we're deleting the messages ourselves, we don't need to send
        # the ReceiptHandle to the caller (it's only used for deleting).
        # If not, we send the entire response.
        if burn_after_reading:
            for m in resp['Messages']:
                yield {k: v for k, v in m.items() if k != 'ReceiptHandle'}
        else:
            yield from resp['Messages']

        # Now delete the messages from the queue, so they won't be read
        # on the next GET call.
        if burn_after_reading:
            logger.info(
                'Deleting %d messages from %s',
                len(resp['Messages']), queue_url)
            client.delete_message_batch(
                QueueUrl=queue_url,
                Entries=[
                    {'Id': m['MessageId'], 'ReceiptHandle': m['ReceiptHandle']}
                    for m in resp['Messages']
                ]
            )


def write_all_messages_to_s3(bucket, key, queue_url):
    """
    Write all the messages from a queue to an S3 bucket.
    """
    messages = []

    def write():
        logger.info(
            'Writing %d messages to s3://%s/%s',
            len(messages), bucket, key)
        s3_utils.write_dicts_to_s3(bucket=bucket, key=key, dicts=messages)

    generator = get_messages(
        queue_url=queue_url, burn_after_reading=True, batch_size=10
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
    queue_url = os.environ['QUEUE_URL']
    queue_name = os.path.basename(queue_url)

    date_string = dt.datetime.now().strftime('%Y-%m-%d_%H-%M-%S')
    key = f'sqs/{queue_name}_{date_string}.txt'
    bucket = os.environ['BUCKET']

    write_all_messages_to_s3(bucket=bucket, key=key, queue_url=queue_url)
