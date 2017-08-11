#!/usr/bin/env python3
# -*- encoding: utf-8 -*-

import collections
from urllib.parse import urljoin

import boto3
import hcl
import requests


Cluster = collections.namedtuple('Cluster', ['url', 'username', 'password'])

# Get a list of the indices defined in Terraform
s3 = boto3.client('s3')
tfvars_obj = s3.get_object(Bucket='platform-infra', Key='terraform.tfvars')
tfvars_body = tfvars_obj['Body'].read()
tfvars = hcl.loads(tfvars_body)

indices_in_use = collections.defaultdict(set)
for key, v in tfvars.items():
    if not key.startswith('es_config_'):
        continue
    url = f'https://{v["name"]}.{v["region"]}.aws.found.io:{v["port"]}'
    cluster = Cluster(url=url, username=v['username'], password=v['password'])
    indices_in_use[cluster].add(v['index'])

# Now ask each cluster: what indices do you know about?
for cluster, indices in indices_in_use.items():
    resp = requests.get(
        urljoin(cluster.url, '/_cat/indices'),
        params={'format': 'json'},
        auth=(cluster.username, cluster.password)
    )

    # Get the names of the indices.  Names beginning with a . are used for
    # internal ES bookkeeping; ignore them.
    index_names = [
        r['index']
        for r in resp.json()
        if not r['index'].startswith('.')
    ]

    # If an index isn't in use, delete it!
    for i in index_names:
        if i not in indices:
            print(f'Deleting unused index {i}')
            resp = requests.delete(
                urljoin(cluster.url, i),
                auth=(cluster.username, cluster.password)
            )
            resp.raise_for_status()
