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


def get_api_terraform_locals(name):
    path = os.path.join(ROOT, 'catalogue_api', 'terraform', name)
    return hcl.loads(open(path).read())['locals']


if __name__ == '__main__':
    api_config = get_api_terraform_locals('api_pins.tf')
    index_config = get_api_terraform_locals('index_config.tf')

    es_config = index_config[f'es_config_{api_config["production_api"]}']

    outpath = os.path.join(ROOT, 'data_api', 'terraform', 'es_config.tf')

    contents = f'''
locals {{
  es_config = {{
    index_v1 = "{es_config["index_v1"]}"
    index_v2 = "{es_config["index_v2"]}"
    doc_type = "{es_config["doc_type"]}"
  }}
}}
'''.lstrip()

    with open(outpath, 'w') as f:
        f.write(contents)
