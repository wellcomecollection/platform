#!/usr/bin/env python
# -*- encoding: utf-8 -*-

import json
import operator
import os
import time

import boto3


def get_new_records(TableName):
    """
    Generates records from the event stream on a DynamoDB table.

    :param TableName: Name of the DynamoDB table.
    """
    client = boto3.client('dynamodbstreams')

    # Look up the stream.  A stream lingers for 24 hours after being destroyed,
    # so there may be more than one stream associated with the table.
    # The StreamLabel parameter is a timestamp for the stream, so we pick
    # the newest stream and trust that this is the current canonical stream.
    stream = max(
        client.list_streams(TableName=TableName)['Streams'],
        key=operator.itemgetter('StreamLabel')
    )

    # Get a shard ID from the stream.
    DescribedStream = client.describe_stream(
        StreamArn=stream['StreamArn']
    )
    ShardId = DescribedStream['StreamDescription']['Shards'][0]['ShardId']

    # 'LATEST' means we always get the most recent data from the shard.
    initial_iterator = client.get_shard_iterator(
        StreamArn=stream['StreamArn'],
        ShardId=ShardId,
        ShardIteratorType='LATEST',
    )
    ShardIterator = initial_iterator['ShardIterator']

    while True:
        result = client.get_records(ShardIterator=ShardIterator)
        print('Received result from DynamoDB stream: %r' % result)
        for record in result['Records']:
            print('Extracted record from DynamoDB stream: %r' % record)
            yield record
        ShardIterator = result['NextShardIterator']
        time.sleep(1)


def _extract_image_from_dynamodb(record):
    """
    Given a record from `get_new_records()`, extract the key-value pairs.
    """
    result = {}
    for k, v in record['dynamodb']['NewImage'].items():
        # The value should be of the form {'T': value}, where 'T' denotes
        # the type of the data in the DynamoDB record.
        assert len(v) == 1
        result[k] = list(v.values())[0]
    result['data'] = json.loads(result['data'])
    print('Extracted data from DynamoDB record: %r' % data)
    return result


def get_unified_item_from_calm(data):
    """
    Given data from a DynamoDB record, turn it into a UnifiedItem.
    """
    d = {
        'identifiers': [{
            'source': 'calm',
            'sourceId': 'MiroID',
            'value': data['RecordID']
        }]
    }
    try:
        d['AccessStatus'] = data['data']['AccessStatus']
    except KeyError:
        pass
    print('Prepared unified item %r' % d)
    return d


if __name__ == '__main__':
    sns = boto3.client('sns')

    for record in get_new_records(os.environ['TABLE_NAME']):
        data = _extract_image_from_dynamodb(record)
        item = get_unified_item_from_calm(data)
        print('Sending item %r to SNS' % item)
        sns.publish(
            TopicArn=os.environ['SNS_TOPIC'],
            Message=json.dumps(item),
            Subject='foo',
        )
