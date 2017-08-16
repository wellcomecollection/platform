#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
Delete unused indexes from Elasticsearch.

This script inspects our Terraform config in S3, identifies which indexes are
in use, and deletes any indexes on the cluster which aren't being used.

Usage:
  elasticsearch_cleaner.py --bucket=<BUCKET> --key=<KEY> [--dry-run]
  elasticsearch_cleaner.py -h | --help

Options:
  --bucket=<BUCKET>     Name of the S3 bucket holding our Terraform config
  --key=<KEY>           Key of our Terraform config in S3
  --dry-run             Only print the name of indexes to delete, don't
                        actually delete anything
  -h --help             Show this screen
"""

import collections
from urllib.parse import urljoin

import boto3
import docopt
import hcl
import requests


Cluster = collections.namedtuple('Cluster', ['url', 'username', 'password'])


def parse_terraform_config(bucket, key):
    """
    Return parsed Terraform config from S3.
    """
    s3 = boto3.client('s3')
    tfvars_obj = s3.get_object(Bucket=bucket, Key=key)
    tfvars_body = tfvars_obj['Body'].read()
    return hcl.loads(tfvars_body)


def identify_es_config_in_tfvars(tfvars):
    """
    Identify the Elasticsearch clusters that are in use in our Terraform.
    """
    known_indexes = collections.defaultdict(set)

    for key, v in tfvars.items():
        # Elasticsearch config blocks are of the form es_config_{variant},
        # e.g. es_config_ingestor, es_config_remus
        if not key.startswith('es_config_'):
            continue

        url = f'https://{v["name"]}.{v["region"]}.aws.found.io:{v["port"]}'
        cluster = Cluster(
            url=url,
            username=v['username'],
            password=v['password']
        )
        known_indexes[cluster].add(v['index'])

    return known_indexes


def find_unused_es_indexes(known_indexes):
    unused_indexes = collections.defaultdict(set)

    for cluster, indexes in known_indexes.items():

        # Ask the cluster for all the indexes it knows about
        resp = requests.get(
            urljoin(cluster.url, '/_cat/indices'),
            params={'format': 'json'},
            auth=(cluster.username, cluster.password)
        )

        # Get the names of the indexes.  Names beginning with a . are used for
        # internal ES bookkeeping; ignore them.
        index_names = [
            r['index']
            for r in resp.json()
            if not r['index'].startswith('.')
        ]

        for i in index_names:
            if i not in indexes:
                unused_indexes[cluster].add(i)

    return unused_indexes


def delete_indexes(unused_indexes, is_dry_run):
    for cluster, indexes in unused_indexes.items():
        for i in indexes:
            print(f'Deleting unused index {i} on cluster {cluster.url}')
            if not is_dry_run:
                resp = requests.delete(
                    urljoin(cluster.url, i),
                    auth=(cluster.username, cluster.password)
                )
                resp.raise_for_status()


if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    bucket = args['--bucket']
    key = args['--key']
    is_dry_run = args['--dry-run']

    print('*** Starting elasticsearch_cleaner.py')
    tfvars = parse_terraform_config(bucket=bucket, key=key)
    known_indexes = identify_es_config_in_tfvars(tfvars)
    unused_indexes = find_unused_es_indexes(known_indexes)
    delete_indexes(unused_indexes, is_dry_run=is_dry_run)
    print('*** Finished elasticsearch_cleaner.py')
