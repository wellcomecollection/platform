#!/usr/bin/env python3
# -*- encoding: utf-8
"""
Create a request to archive a bag

Usage: trigger_archive_bag.py <BAG>... [--bucket=<BUCKET_NAME>] [--topic=<TOPIC_NAME>|--api=<API>] [--sns] [--insecure]
       trigger_archive_bag.py -h | --help

Arguments:
    BAG                    BagIt files to ingest

Examples:
    trigger_archive_bag.py b22454408.zip
    trigger_archive_bag.py b22454408.zip --sns

Options:
    --bucket=<BUCKET_NAME>  The S3 bucket containing the bags.
                            [default: wellcomecollection-assets-archive-ingest]
    --topic=<TOPIC_NAME>    The archivist topic.
                            [default: archive-storage_archivist]
    --api=<API>             The API endpoint to use
                            [default: http://api.wellcomecollection.org/storage/v1/ingests]
    --sns                   Send directly to SNS rather than through the API
    --insecure              Allow insecure connections to the API
    -h --help               Print this help message
"""
import uuid

import boto3
import docopt
import json
import requests


def archive_bag_sns_messages(bags, bucket):
    """
    Generates bag archive request messages.
    """
    for bag in bags:
        request_id = str(uuid.uuid4())
        yield {
            'archiveRequestId': request_id,
            'zippedBagLocation': {
                'namespace': bucket,
                'key': bag
            }
        }


def archive_bag_api_messages(bags, bucket):
    """
    Generates bag archive messages.
    """
    for bag in bags:
        yield {
            'type': 'Ingest',
            'ingestType': {
                'id': 'create',
                'type': 'IngestType'
            },
            'uploadUrl': f's3://{bucket}/{bag}'
        }


def build_topic_arn(topic_name):
    """Given a topic name, return the topic ARN."""
    # https://stackoverflow.com/a/37723278/1558022
    sts_client = boto3.client('sts')
    account_id = sts_client.get_caller_identity().get('Account')

    return f'arn:aws:sns:eu-west-1:{account_id}:{topic_name}'


def publish_messages(topic_arn, messages):
    """Publish a sequence of messages to an SNS topic."""
    sns_client = boto3.client('sns')
    for m in messages:
        message_as_json = json.dumps(m)
        response = sns_client.publish(
            TopicArn=topic_arn,
            MessageStructure='json',
            Message=json.dumps({
                'default': message_as_json
            }),
            Subject=f'Source: {__file__}'
        )
        response_status = response['ResponseMetadata']['HTTPStatusCode']
        print(f'{message_as_json} -> {topic_arn} [{response_status}]')
        assert response_status == 200, response


def publish_to_sns(bucket_name, bags, topic_name):
    topic_arn = build_topic_arn(topic_name)

    publish_messages(
        topic_arn=topic_arn,
        messages=archive_bag_sns_messages(bags, bucket_name)
    )


def call_ingest_api(bucket_name, bags, api, verify_ssl_certificate=True):
    session = requests.Session()
    for message in archive_bag_api_messages(bags, bucket_name):
        response = session.post(api, json=message, verify=verify_ssl_certificate)
        status_code = response.status_code
        if status_code != 202:
            print_result(f'ERROR calling {api}', response)
        else:
            print(f'{message} -> {api} [{status_code}]')
            location = response.headers.get('Location')
            ingest = session.get(location, verify=verify_ssl_certificate)
            if location:
                print_result(location, ingest)


def print_result(description, result):
    print(description)
    dumped_json = json.dumps(result.json(), indent=2)
    print(dumped_json)


def main():
    args = docopt.docopt(__doc__)
    bags = args['<BAG>']
    bucket_name = args['--bucket']
    use_sns_directly = args['--sns']
    insecure_api = args['--insecure']

    if use_sns_directly:
        topic_name = args['--topic']
        publish_to_sns(bucket_name, bags, topic_name)
    else:
        api = args['--api']
        call_ingest_api(bucket_name, bags, api, not insecure_api)


if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        import sys

        sys.exit(1)
