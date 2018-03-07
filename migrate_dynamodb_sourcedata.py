#!/usr/bin/env python
# -*- encoding: utf-8 -*-

import json
import re

import boto3


dynamodb = boto3.resource('dynamodb')
table = dynamodb.Table('SourceData')


def items():
    """Generate all items from a DynamoDB table."""
    kwargs = {}
    while True:
        resp = table.scan(**kwargs)
        yield from resp['Items']
        kwargs['ExclusiveStartKey'] = resp['LastEvaluatedKey']
        break


def transform_item(item):
    print(f'Processing {item["id"]}')

    prefix, id = item['id'].split('/')
    if prefix == 'sierra':
        new_id = id.lstrip('b')
        assert re.match(r'^\d{7}$', new_id)
        item['id'] = '/'.join([prefix, new_id])

    return item


for item in items():
    old_item = item.copy()
    new_item = transform_item(item)
    if old_item == new_item:
        continue
    print(new_item)
    # table.put_item(Item=new_item)
    # table.delete_item(Key={'id': item['id']})
    break
