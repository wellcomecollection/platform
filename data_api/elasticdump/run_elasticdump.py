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


def build_elasticsearch_url(environ_config, index):
    """
    Build an Elasticsearch URL from environment variables and index name.
    """
    username = environ_config['es_username']
    password = environ_config['es_password']
    hostname = environ_config['es_hostname']
    port = environ_config['es_port']

    # The production scheme should always be HTTPS, but we need to be able
    # to use HTTP with our Docker image in tests.
    scheme = environ_config.get('es_scheme', 'https://')

    return f'{scheme}{username}:{password}@{hostname}:{port}/{index}'


def aws_client(service_name):
    """
    Helper method for getting AWS clients.  In particular, this looks for
    environment variables which let us overwrite the endpoint URLs.
    """
    try:
        return boto3.client(
            service_name,
            endpoint_url=os.environ[f'local_{service_name}_endpoint']
        )
    except KeyError:
        return boto3.client(service_name)


@service
def run():
    print(os.environ)
    sqs_queue_url = os.environ['sqs_queue_url']

    # TODO: Currently we read the target bucket from the environment config.
    # It would be nice to support overriding the bucket name with config from
    # the SQS message.
    target_bucket = os.environ['upload_bucket']

    s3_client = aws_client('s3')
    sqs_client = aws_client('sqs')

    print('*** Reading messages from SQS')
    message = get_message(sqs_client=sqs_client, sqs_queue_url=sqs_queue_url)

    print('*** Constructing Elasticsearch URL')

    # TODO: Currently we read the index name from the environment config.
    # It would be nice to support overriding the index name with config from
    # the SQS message.
    es_index = os.environ['es_index']

    es_url = build_elasticsearch_url(environ_config=os.environ, index=es_index)

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

    prefix = os.environ['key_prefix']
    # This creates keys of the form
    #
    #   2018/03/2018-03-13_myindexname.txt.gz
    #
    # which are human-readable, unambiguous, and easy to browse in the
    # S3 Console.
    try:
        key = os.environ['target_key']
    except KeyError:
        key = dt.datetime.now().strftime('%Y/%m/%Y-%m-%d') + f'_{es_index}.txt.gz'
    print(f'*** Uploading gzip file to S3 with key {key}')

    s3_client.upload_file(
        Bucket=target_bucket,
        Key=prefix + key,
        Filename='index.txt.gz'
    )

    print('*** Deleting the SQS message')
    sqs_client.delete_message(
        QueueUrl=sqs_queue_url,
        ReceiptHandle=message['ReceiptHandle']
    )


if __name__ == '__main__':
    run()
