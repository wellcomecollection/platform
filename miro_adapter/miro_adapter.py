#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Parse image records from a Miro export and push them into a DynamoDB table.

Usage:
  miro_adapter.py --table=<TABLE> --collection=<COLLECTION> --bucket=<BUCKET> --key=<KEY>
  miro_adapter.py -h | --help

Options:
  -h --help                   Show this screen.
  --table=<TABLE>             DynamoDB table to write the Miro data to.
  --collection=<COLLECTION>   Name of the associated Miro images collection.
  --bucket=<BUCKET>           S3 bucket containing the Miro XML dumps.
  --key=<KEY>                 Key of the Miro XML dump in the S3 bucket.

"""

import json
import time

import boto3
from botocore.exceptions import ClientError
import docopt

from utils import generate_images


def push_to_dynamodb(table_name, collection_name, image_data):
    """
    Given the name of a Dynamo table and some image data, push it
    into DynamoDB.
    """
    dynamodb = boto3.resource('dynamodb')
    table = dynamodb.Table(table_name)

    wait_time = 1

    with table.batch_writer() as batch:
        for i, image in enumerate(image_data, start=1):
            print('Pushing image %d with ID %s' % (i, image['image_no_calc']))
            batch.put_item(
                Item={
                    'MiroID': image['image_no_calc'],
                    'MiroCollection': collection_name,
                    'ReindexShard': 'default',
                    'ReindexVersion': 1,
                    'data': json.dumps(image, separators=(',', ':'))
                }
            )
            if i % 50 == 0:
                time.sleep(5)


if __name__ == '__main__':
    args = docopt.docopt(__doc__)
    image_data = generate_images(bucket=args['--bucket'], key=args['--key'])
    push_to_dynamodb(
        table_name=args['--table'],
        collection_name=args['--collection'],
        image_data=image_data
    )
