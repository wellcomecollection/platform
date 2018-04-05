#!/usr/bin/env python
# -*- encoding: utf-8

import datetime as dt
import gzip
import os
import subprocess
import sys

import boto3

from service_utils import service


def get_message(sqs_client, sqs_queue_url):
    resp = sqs_client.receive_message(
        QueueUrl=sqs_queue_url,
        MaxNumberOfMessages=1
    )

    try:
        message = resp['Messages'][0]
    except (KeyError, IndexError):
        print('*** No messages received from SQS, aborting!')
        sys.exit(0)
    else:
        print(f'*** Received message {message!r} from SQS')
        return message


def build_elasticsearch_url(index):
    # We read the Elasticsearch auth information from the environment variables
    username = os.environ['es_username']
    password = os.environ['es_password']
    hostname = os.environ['es_hostname']
    port = os.environ['es_port']
    scheme = os.environ.get('es_scheme', 'http://')

    return f'{scheme}{username}:{password}@{hostname}:{port}/{index}'


@service
def run():
    print(os.environ)
    sqs_queue_url = os.environ['sqs_queue_url']

    # TODO: Currently we read the target bucket from the environment config.
    # It would be nice to support overriding the bucket name with config from
    # the SQS message.
    target_bucket = os.environ['upload_bucket']

    if 'local_s3_endpoint' in os.environ:
        s3_client = boto3.client('s3', endpoint_url=os.environ['local_s3_endpoint'])
    else:
        s3_client = boto3.client('s3')

    if 'local_sqs_endpoint' in os.environ:
        sqs_client = boto3.client('sqs', endpoint_url=os.environ['local_sqs_endpoint'])
    else:
        sqs_client = boto3.client('sqs')

    print('*** Reading messages from SQS')
    message = get_message(sqs_client=sqs_client, sqs_queue_url=sqs_queue_url)

    print('*** Constructing Elasticsearch URL')

    # TODO: Currently we read the index name from the environment config.
    # It would be nice to support overriding the index name with config from
    # the SQS message.
    es_index = os.environ['es_index']

    es_url = build_elasticsearch_url(index=es_index)

    print('*** Running Elasticdump task')
    try:
        subprocess.check_call([
            'elasticdump',
            '--input', es_url,
            '--output', 'index.txt',

            # How many records to fetch in each request
            '--limit', '1000',
        ])
    except subprocess.CalledProcessError:
        sys.exit(1)
    assert os.path.exists('index.txt')

    print('*** Created index file; compressing as gzip')
    with open('index.txt', 'rb') as index_file:
        with gzip.open('index.txt.gz', 'wb') as gzip_file:
            gzip_file.writelines(index_file)

    # This creates keys of the form
    #
    #   2018/03/2018-03-13_myindexname.txt.gz
    #
    # which are human-readable, unambiguous, and easy to browse in the
    # S3 Console.
    key = dt.datetime.now().strftime('%Y/%m/%Y-%m-%d') + f'_{es_index}.txt.gz'
    print(f'*** Uploading gzip file to S3 with key {key}')

    s3_client.upload_file(
        Bucket=target_bucket,
        Key=key,
        Filename='index.txt.gz'
    )

    print('*** Deleting the SQS message')
    sqs_client.delete_message(
        QueueUrl=sqs_queue_url,
        ReceiptHandle=message['ReceiptHandle']
    )


if __name__ == '__main__':
    run()
