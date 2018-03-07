#!/usr/bin/env python
# -*- encoding: utf-8 -*-

import json
import re

import boto3


dynamodb = boto3.resource('dynamodb')
table = dynamodb.Table('SierraData_items')


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

    item['id'] = item['id'].lstrip('i')
    assert re.match(r'^\d{7}$', item['id'])

    for bib_id_key in ('bibIds', 'unlinkedBibIds'):
        item[bib_id_key] = [bib_id.lstrip('b') for bib_id in item[bib_id_key]]
        assert all(re.match(r'^\d{7}$', bib_id) for bib_id in item[bib_id_key])

    data = json.loads(item['data'])
    data['id'] = data['id'].lstrip('i')
    assert re.match(r'^\d{7}$', data['id'])

    data['bibIds'] = [bib_id.lstrip('b') for bib_id in data['bibIds']]
    assert all(re.match(r'^\d{7}$', bib_id) for bib_id in data['bibIds'])

    item['data'] = json.dumps(data, separators=(',', ':'))

    return item


for item in items():
    old_item = item.copy()
    new_item = transform_item(item)
    if old_item == new_item:
        continue
    table.put_item(Item=new_item)
    # table.delete_item(Key={'id': item['id']})
    break
