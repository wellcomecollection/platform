#!/usr/bin/env python3
# -*- encoding: utf-8
"""
Create a request to archive a bag

Usage: trigger_archive_bag.py <BAG>... [--bucket=<BUCKET_NAME>] [--topic=<TOPIC_NAME>|--api=<API>] [--sns=(true|false)]
       trigger_archive_bag.py -h | --help

Options:
  --bucket=<BUCKET_NAME> The S3 bucket containing the bags.
                         [default: wellcomecollection-assets-archive-ingest]
  --topic=<TOPIC_NAME>   The archivist topic.
                         [default: archive-storage_archivist]
   --api=<API>           The API endpoint to use
                         [default: http://api.wellcomecollection.org/prod/storage/v1/ingests]
   --sns=(true|false)    Send directly to SNS rather than through the API
                         [default: false]
  -h --help              Print this help message
"""

import docopt
import boto3
import requests
import json


def archive_bag_sns_messages(bags, bucket):
    """
    Generates bag archive messages.
    """
    for bag in bags:
        yield {
            'namespace': bucket,
            'key': bag
        }


def archive_bag_api_messages(bags, bucket):
    """
    Generates bag archive messages.
    """
    for bag in bags:
        yield {
            'uploadUrl': f"s3://{bucket}/{bag}"
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


def call_ingest_api(bucket_name, bags, api):
    session = requests.Session()
    for message in archive_bag_api_messages(bags, bucket_name):
        response = session.post(api, json=message)
        print(f'{message} -> {api} [{response.status_code}] {response.json()}')


def main():
    args = docopt.docopt(__doc__)
    bags = args['<BAG>']
    bucket_name = args['--bucket']
    use_sns_directly = args['--sns']

    if use_sns_directly.lower() == "true":
        topic_name = args['--topic']
        publish_to_sns(bucket_name, bags, topic_name)
    else:
        api = args['--api']
        call_ingest_api(bucket_name, bags, api)


if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        import sys
        sys.exit(1)
