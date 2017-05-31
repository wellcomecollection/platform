#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
This is a diagnostic script for finding Miro records that have been
pushed into DynamoDB but aren't available in the API -- which may be an
indicator that the pipeline is leaking somewhere.
"""

import pprint
import sys

import boto3
import requests


def api_miro_ids():
    """
    Generates the Miro IDs for records that are available in the API.
    """
    page = 1
    while True:
        r = requests.get(
            'https://api.wellcomecollection.org/catalogue/v0/works',
            params={'includes': 'identifiers', 'pageSize': 100, 'page': page}
        )
        if not r.json()['results']:
            break
        for work in r.json()['results']:
            identifiers = work['identifiers']
            miro_ids = [i for i in identifiers if i['source'] == 'Miro']
            if miro_ids:
                yield miro_ids[0]['value']
        page += 1


def get_records(table):
    """
    Retrieve all the records in a DynamoDB table.
    """
    kwargs = {}
    while True:
        resp = table.scan(**kwargs)
        yield from resp['Items']

        # DynamoDB results are paginated, with the ``LastEvaluatedKey`` in
        # the response defining a parameter to be passed into the next page,
        # as the start of the next response.  When it's no longer present,
        # we're at the end of the table.  For more details:
        # http://boto3.readthedocs.io/en/latest/reference/services/dynamodb.html#DynamoDB.Table.scan
        try:
            kwargs['ExclusiveStartKey'] = resp['LastEvaluatedKey']
        except KeyError:
            break


def main():
    dynamodb = boto3.resource('dynamodb')
    table = dynamodb.Table('MiroData')

    api_ids = list(api_miro_ids())
    dynamodb_records = [t['MiroID'] for t in get_records(table)]

    missing = set(dynamodb_records) - set(api_ids)
    if missing:
        pprint.pprint(missing)
        return 1
    else:
        return 0


if __name__ == '__main__':
    sys.exit(main())
