#!/usr/bin/env python
# -*- encoding: utf-8 -*-

import json
import re
import sys

import boto3


dynamodb = boto3.resource('dynamodb')
table = dynamodb.Table('SierraData_items')


def items(kwargs=None):
    """Generate all items from a DynamoDB table."""
    if kwargs is None:
        kwargs = {}
    while True:
        resp = table.scan(**kwargs)
        yield from resp['Items']
        kwargs['ExclusiveStartKey'] = resp['LastEvaluatedKey']


def transform_item(item):
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



def main():
    try:
        kwargs = {'ExclusiveStartKey': {'id': sys.argv[1]}}
    except IndexError:
        kwargs = {}

    for item in items(kwargs):
        print(f'Starting {item["id"]}')
        old_item = item.copy()
        new_item = transform_item(item)
        if old_item == new_item:
            print(f'Skipping {item["id"]}')
            continue
        print(f'Processing {item["id"]}')
        table.put_item(Item=new_item)
        table.delete_item(Key={'id': old_item['id']})


if __name__ == '__main__':
    main()
