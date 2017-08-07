#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
Handles creating or destroying an AWS Batch compute environment

Usage:
  aws_batch_compute.py [--create=<location>]
  aws_batch_compute.py [--destroy=<name>]
  aws_batch_compute.py -h | --help

Options:
  -h --help                Show this screen.
  --create=<file>          Location of compute environment json description to create
  --destroy=<name>         Name of the compute environment to destroy
"""

import json

import boto3
import docopt



if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    create = args['--create']
    destroy = args['--destroy']

    client = boto3.client('batch')

    if create is not None:
        create_description = open(create).read()
        parsed_description = json.loads(create_description)

        client.create_compute_environment(**parsed_description)
    else:
        compute_env_name = destroy

        client.delete_compute_environment(
            computeEnvironment=compute_env_name
        )