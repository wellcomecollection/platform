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

import boto3
import docopt

from utils import generate_images


def push_to_dynamodb(table_name, collection_name, image_data):
    """
    Given the name of a Dynamo table and some image data, push it
    into DynamoDB.
    """
    dynamodb = boto3.resource('dynamodb')
    table = dynamodb.Table(table_name)

    with table.batch_writer() as batch:
        for idx, image in enumerate(image_data, start=1):
            print('Pushing image with ID %s' % image['image_no_calc'])
            batch.put_item(
                Item={
                    'MiroID': image['image_no_calc'],
                    'MiroCollection': collection_name,
                    'ReindexShard': 'default',
                    'ReindexVersion': 0,
                    'data': json.dumps(image, separators=(',', ':'))
                }
            )
        print('Written %d records to DynamoDB' % idx)


if __name__ == '__main__':
    args = docopt.docopt(__doc__)
    image_data = generate_images(bucket=args['--bucket'], key=args['--key'])
    push_to_dynamodb(
        table_name=args['--table'],
        collection_name=args['--collection'],
        image_data=image_data
    )
