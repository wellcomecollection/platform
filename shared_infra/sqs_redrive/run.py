#!/usr/bin/env python
# -*- encoding: utf-8 -*-
import logging
import os

import boto3
import daiquiri

daiquiri.setup(level=logging.INFO)
logger = daiquiri.getLogger(__name__)


def send_message(sqs_client, msg, queue_url):
    sqs_client.send_message(
        QueueUrl=queue_url,
        MessageBody=msg
    )


def delete_message(sqs_client, msg, queue_url):
    sqs_client.delete_message(
        QueueUrl=queue_url,
        ReceiptHandle=msg['ReceiptHandle']
    )


def main(sqs_client):
    source_queue_url = os.environ["SQS_SOURCE_URL"]
    target_queue_url = os.environ["SQS_TARGET_URL"]
    while True:
        response = sqs_client.receive_message(
            QueueUrl=source_queue_url
        )

        if "Messages" not in response:
            break

        for msg in response["Messages"]:
            send_message(sqs_client, msg['Body'], target_queue_url)
            delete_message(sqs_client, msg, source_queue_url)

    logging.info("Done")


if __name__ == '__main__':
    sqs_client = boto3.client('sqs')
    main(sqs_client)