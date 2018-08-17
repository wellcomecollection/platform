#!/usr/bin/env python
# -*- encoding: utf-8
"""
This script updates the Terraform configuration in the data_api stack
to use whatever Elasticsearch index is in use in the catalogue_api.
"""

import os
import subprocess

import hcl


ROOT = subprocess.check_output(
    ['git', 'rev-parse', '--show-toplevel']).decode('utf8').strip()


def api_terraform(name):
    path = os.path.join(ROOT, 'catalogue_api', 'terraform', name)
    return hcl.loads(open(path).read())


if __name__ == '__main__':
    api_config = api_terraform('api_config.tf')
    index_config = api_index_config('index_config.tf')

    from pprint import *
    pprint(api_config)
    pprint(index_config)
