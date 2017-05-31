#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Parse image records from a Miro export and push them into a DynamoDB table.
"""

import argparse
import json

import boto3
from lxml import etree

from utils import elem_to_dict


def parse_args():
    """Parse command-line arguments."""
    parser = argparse.ArgumentParser(__doc__.strip())

    parser.add_argument(
        '--table', help='Name of the DynamoDB table', required=True)
    parser.add_argument(
        '--collection', help='Name of the Miro collection', required=True)
    parser.add_argument(
        'EXPORT_PATH', help='Path to the Miro export file')

    return parser.parse_args()


def parse_image_data(xml_string):
    """
    Given an XML string, generate blobs of data for each image.
    """
    # Within the Miro XML file, each image is stored inside in the
    # following format:
    #
    #   <image>
    #     <key1>value1</key1>
    #     <key2>value1</key2>
    #     ...
    #   </image>
    #
    # so we want to grab the <image> tags, and export data from their
    # children attributes.
    root = etree.fromstring(xml_string)
    for child in root.findall('image'):
        yield elem_to_dict(child)


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
    args = parse_args()
    image_data = parse_image_data(open(args.EXPORT_PATH, 'rb').read())
    push_to_dynamodb(
        table_name=args.table,
        collection_name=args.collection,
        image_data=image_data
    )
