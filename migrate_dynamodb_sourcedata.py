#!/usr/bin/env python
# -*- encoding: utf-8 -*-

import json
import os
import re
import sys

import boto3
import mmh3


s3_client = boto3.client('s3')

dynamodb = boto3.resource('dynamodb')
table = dynamodb.Table('SourceData')


def items(kwargs=None):
    """Generate all items from a DynamoDB table."""
    if kwargs is None:
        kwargs = {}
    while True:
        resp = table.scan(**kwargs)
        yield from resp['Items']
        kwargs['ExclusiveStartKey'] = resp['LastEvaluatedKey']


def _transform_itemdata(itemdata):
    new_item_data = {}
    for k, v in itemdata.items():
        v['id'] = v['id'].lstrip('i')
        assert re.match(r'^\d{7}$', v['id'])

        for bib_id_key in ('bibIds', 'unlinkedBibIds'):
            v[bib_id_key] = [bib_id.lstrip('b') for bib_id in v[bib_id_key]]
            assert all(re.match(r'^\d{7}$', bib_id) for bib_id in v[bib_id_key])

        v_data = json.loads(v['data'])
        v_data['id'] = v_data['id'].lstrip('i')
        assert re.match(r'^\d{7}$', v_data['id'])

        v_data['bibIds'] = [bib_id.lstrip('b') for bib_id in v_data['bibIds']]
        assert all(re.match(r'^\d{7}$', bib_id) for bib_id in v_data['bibIds'])

        v['data'] = json.dumps(v_data, separators=(',', ':'))

        new_item_data[v['id']] = v

    return new_item_data


def transform_s3_data(json_string):
    data = json.loads(json_string)

    assert data['sourceName'] == 'sierra'

    data['sourceId'] = data['sourceId'].lstrip('b')
    assert re.match(r'^\d{7}$', data['sourceId'])

    data['itemData'] = _transform_itemdata(data['itemData'])

    assert data['maybeBibData']
    data['maybeBibData']['id'] = data['maybeBibData']['id'].lstrip('b')
    assert re.match(r'^\d{7}$', data['maybeBibData']['id'])

    bib_data = json.loads(data['maybeBibData']['data'])
    bib_data['id'] = bib_data['id'].lstrip('b')
    assert re.match(r'^\d{7}$', bib_data['id'])
    data['maybeBibData']['data'] = json.dumps(bib_data, separators=(',', ':'))

    return json.dumps(data, separators=(',', ':')).encode('utf8')


def transform_item(item):

    prefix, id = item['id'].split('/')
    if prefix == 'sierra':
        new_id = id.lstrip('b')
        assert re.match(r'^\d{7}$', new_id)
        item['id'] = '/'.join([prefix, new_id])
        item['sourceId'] = new_id

        s3_json_string = s3_client.get_object(
            Bucket='wellcomecollection-vhs-sourcedata',
            Key=item['s3key'])['Body'].read()
        new_s3_json_string = transform_s3_data(s3_json_string)

        if s3_json_string != new_s3_json_string:
            s3_hash = mmh3.hash(new_s3_json_string)
            new_key = os.path.join(
                os.path.dirname(os.path.dirname(item['s3key'])),
                new_id,
                f'{s3_hash}.json'
            )
            s3_client.put_object(
                Bucket='wellcomecollection-vhs-sourcedata',
                Key=new_key,
                Body=new_s3_json_string
            )

            item['s3key'] = new_key

    return item


from botocore.exceptions import ClientError
from tenacity import *


@retry(
    retry=retry_if_exception_type(ClientError),
    wait=wait_exponential(multiplier=1, max=10)
)
def main():
    try:
        kwargs = {'ExclusiveStartKey': {'id': sys.argv[1]}}
    except IndexError:
        kwargs = {}
    for item in items(kwargs):

        old_item = item.copy()
        new_item = transform_item(item)
        if old_item == new_item:
            print('.', end='', flush=True)
            continue
        print(f'Processing {old_item["id"]}')
        table.put_item(Item=new_item)
        table.delete_item(Key={'id': old_item['id']})
        s3_client.delete_object(
            Bucket='wellcomecollection-vhs-sourcedata',
            Key=old_item['s3key']
        )
    # break


if __name__ == '__main__':
    main()
