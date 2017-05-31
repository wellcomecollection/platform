#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
This scripts looks for duplicate identifiers in the "Identifiers" table.

The caller passes the name of one of the source ID indexes (e.g. MiroID),
and the script looks at every instance of that source ID.  If a source ID
has multiple canonical IDs, it prints information about it to stdout
and exits with code 1.  Otherwise exits with code 0.
"""

import collections
import sys

import boto3


def get_records(table, index_name):
    """
    Retrieve all the records in a DynamoDB table.
    """
    kwargs = {'IndexName': index_name}
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


def build_id_cache(records, index_name):
    """
    Given a series of records from DynamoDB, produce a mapping from their
    source IDs to their canonical IDs.
    """
    id_cache = collections.defaultdict(set)
    for r in records:
        id_cache[r[index_name]].add(r['CanonicalID'])
    return dict(id_cache)


def main():
    if len(sys.argv) != 2:
        sys.exit(f"Usage: {sys.argv[0]} <index_name>")

    index_name = sys.argv[1]

    dynamodb = boto3.resource('dynamodb')
    table = dynamodb.Table('Identifiers')

    id_cache = build_id_cache(
        records=get_records(table, index_name=index_name),
        index_name=index_name
    )

    duplicates = [
        (orig_id, canonical_ids)
        for orig_id, canonical_ids in sorted(id_cache.items())
        if len(canonical_ids) > 1]

    if duplicates:
        for orig_id, canonical_ids in duplicates:
            print(f'{orig_id}\t{canonical_ids}')
        return 1
    else:
        return 0


if __name__ == '__main__':
    sys.exit(main())
