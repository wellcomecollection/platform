#!/usr/bin/env python
# -*- encoding: utf-8

import boto3


def get_existing_records(dynamodb_client):
    """
    Generates existing Miro records from the SourceData table.
    """
    paginator = dynamodb_client.get_paginator('scan')
    for page in paginator.paginate(TableName='SourceData'):
        for item in page['Items']:
            yield item


if __name__ == '__main__':
    dynamodb_client = boto3.client('dynamodb')

    for item in get_existing_records(dynamodb_client):
        print(item)
        break
