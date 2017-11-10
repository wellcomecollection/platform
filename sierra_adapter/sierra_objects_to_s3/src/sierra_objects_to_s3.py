#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Extract objects from Sierra and write thm to S3.

Usage:
  sierra_objects_to_s3.py --type=<TYPE> --url=<URL> --key=<KEY> \
                          --sec=<SECRET> --bucket=<BUCKET> --path=<PATH> \
                          [--from=<FROM>] [--to=<TO>]
  sierra_objects_to_s3.py -h | --help

Options:
  -h --help                 Show this screen.
  --type                    Object type to get from Sierra e.g. '/bibs', '/items'
  --bucket=<BUCKET>         S3 bucket to sync objects to
  --url=<URL>               Base URL of the Sierra API
  --key=<KEY>               Sierra OAuth Key
  --sec=<SEC>               Sierra OAuth Secret
  --from=<FROM>             Maya parseable date string to start from
  --to=<TO>                 Maya parseable date string to end at



"""

import json
import logging

import boto3
import daiquiri
import docopt
import maya

import sierra_api

daiquiri.setup(level=logging.INFO)
logger = daiquiri.getLogger(__name__)


def _build_iso_time(time):
    if time:
        return maya.when(time).iso8601()

    return ""


def build_from_to_params(from_time, to_time):
    iso_from_time = _build_iso_time(from_time)
    iso_to_time = _build_iso_time(to_time)

    return {
        'updatedDate': f'[{iso_from_time},{iso_to_time}]'
    }


def _write_object_to_s3(s3_client, bucket, key, object):
    logger.info(f'Writing object to s3://{bucket}/{key}')

    json_str = json.dumps(
        object,
        sort_keys=True,
        separators=(',', ':')
    ).encode('ascii')

    s3_client.put_object(
        Bucket=bucket,
        Key=key,
        Body=json_str
    )


def write_objects_to_s3(s3_client, bucket, path, objects, id_key='id'):
    for index, object in enumerate(objects):
        id = object[id_key]
        key = f'{path}/{id}.json'

        _write_object_to_s3(s3_client, bucket, key, object)


def main(args, s3_client, sess=None):
    url = args['--url']
    key = args['--key']
    sec = args['--sec']

    object_type = args['--type']

    bucket = args['--bucket']
    path = args['--path']

    from_time = args['--from']
    to_time = args['--to']

    params = build_from_to_params(from_time, to_time)

    objects = sierra_api.SierraAPI(
        url,
        key,
        sec,
        sess
    ).get_objects(
        path=object_type,
        params=params
    )

    write_objects_to_s3(
        s3_client=s3_client,
        bucket=bucket,
        path=path,
        objects=objects
    )


if __name__ == '__main__':
    main(
        docopt.docopt(__doc__),
        boto3.client('s3')
    )
