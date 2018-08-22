#!/usr/bin/env python
# -*- encoding: utf-8

import boto3


OLD_TABLE = 'SourceData'
OLD_BUCKET = 'wellcomecollection-vhs-sourcedata'

NEW_TABLE = 'wellcomecollection-vhs-sourcedata-miro'
NEW_BUCKET = 'wellcomecollection-vhs-sourcedata-miro'


def get_existing_records(dynamodb_client):
    """
    Generates existing Miro records from the SourceData table.
    """
    paginator = dynamodb_client.get_paginator('scan')
    for page in paginator.paginate(TableName=OLD_TABLE):
        for item in page['Items']:
            if 'reindexShard' not in item:
                print(item)

            if item['sourceName'] != {'S': 'miro'}:
                continue
            yield item


if __name__ == '__main__':
    dynamodb_client = boto3.client('dynamodb')
    s3_client = boto3.client('s3')

    for item in get_existing_records(dynamodb_client):
        del item['sourceName']

        s3_client.copy_object(
            Bucket=NEW_BUCKET,
            Key=item['s3key']['S'].replace('miro/', ''),
            CopySource={
                'Bucket': OLD_BUCKET,
                'Key': item['s3key']['S']
            }
        )

        print(item)
        break
