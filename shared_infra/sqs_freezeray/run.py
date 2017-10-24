#!/usr/bin/env python
# -*- encoding: utf-8 -*-

import json

import boto3


def get_messages(queue_url, delete=False, batch_size=10):
    """
    Gets messages from an SQS queue.  If ``delete`` is True, the
    messages are also deleted after they've been read.
    """
    client = boto3.client('sqs')
    while True:
        # We batch message responses to reduce load on the SQS API.
        # Note: 10 is currently the most messages you can read at once.
        resp = client.receive_message(
            QueueUrl=queue_url,
            MaxNumberOfMessages=batch_size
        )

        # If there's nothing available, the queue is empty.  Abort!
        if 'Messages' not in resp:
            break

        # If we're deleting the messages ourselves, we don't need to send
        # the ReceiptHandle to the caller (it's only used for deleting).
        # If not, we send the entire response.
        if delete:
            for m in resp['Messages']:
                yield {k: v for k, v in m.items() if k != 'ReceiptHandle'}
        else:
            yield from resp['Messages']

        # Now delete the messages from the queue, so they won't be read
        # on the next GET call.
        if delete:
            client.delete_message_batch(
                QueueUrl=queue_url,
                Entries=[
                    {'Id': m['MessageId'], 'ReceiptHandle': m['ReceiptHandle']}
                    for m in messages
                ]
            )


def write_to_s3(bucket, key, messages):
    """
    Given a list of messages from SQS, write them, one-per-line, to S3.
    """
    client = boto3.client('s3')
    json_str = b'\n'.join([json.dumps(m).encode('ascii') for m in messages])
    client.put_object(Bucket=bucket, Key=key, Body=json_str)



def write_all_messages_to_s3(bucket, key, queue_url):
    """
    Write all the messages from a queue to an S3 bucket.
    """
    messages = []

    def write():
        write_to_s3(bucket=bucket, key=key, messages=messages)

    generator = get_messages(queue_url=queue_url, delete=True, batch_size=10)
    for i, message in enumerate(generator):

        # Because messages are deleted after they're processed, and received
        # in batches of 10, we write to S3 after every 10 messages.  If the
        # task exits halfway through, we haven't lost data.
        if i % 10 == 0:
            write()

    write()




if __name__ == '__main__':
    bucket = 'platform-infra'
    key = 'sqs/alex-test-queue.txt'
    queue_url = 'https://sqs.eu-west-1.amazonaws.com/760097843905/alex-test-queue'

    write_all_messages_to_s3(bucket=bucket, key=key, queue_url=queue_url)
