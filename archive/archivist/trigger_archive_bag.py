#!/usr/bin/env python3
# -*- encoding: utf-8
"""
Create a request to archive a bag

Usage: trigger_archive_bag.py <BAG>... [--bucket=<BUCKET_NAME>]  [--topic=<TOPIC_NAME>]
       trigger_archive_bag.py -h | --help

Options:
  --bucket=<BUCKET_NAME> The S3 bucket containing the bags.
                         [default: wellcomecollection-assets-archive-ingest]
  --topic=<TOPIC_NAME>   The archivist topic.
                         [default: archive-storage_archivist]
  -h --help              Print this help message
"""

import docopt
import boto3
import json


def archive_bag_messages(bags, bucket):
    """
    Generates bag archive messages.
    """
    for bag in bags:
        yield {
            'namespace': bucket,
            'key': bag
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


def main():
    args = docopt.docopt(__doc__)

    bags = args['<BAG>']
    bucket_name = args['--bucket']
    messages = archive_bag_messages(bags, bucket_name)

    topic_name = args['--topic']
    # print(f'topic: {topic_name} bucket: {bucket_name}')
    topic_arn = build_topic_arn(topic_name)

    publish_messages(
        topic_arn=topic_arn,
        messages=messages
    )


if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        import sys
        sys.exit(1)
