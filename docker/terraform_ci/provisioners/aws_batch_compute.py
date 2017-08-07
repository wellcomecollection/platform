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

def _environment_exists(client, name):
    response = client.describe_compute_environments(
        computeEnvironments=[
            name,
        ],
        maxResults=1,
    )

    return response["computeEnvironments"].length > 0


def _parse_json_file(file):
    file_contents = open(file).read()

    return json.loads(file_contents)


def create_compute_environment(client, description):
    name = description["computeEnvironmentName"]

    if not _environment_exists(client, name):
        client.create_compute_environment(**description)


def delete_compute_environment(name):
    if _environment_exists(client, name):
        client.delete_compute_environment(
            computeEnvironment=name
        )


if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    description_file_location = args['--create']
    compute_env_name = args['--destroy']

    client = boto3.client('batch')

    if create is not None:
        description = _parse_json_file(description_file_location)
        create_compute_environment(client, description)
    else:
        delete_compute_environment(client, compute_env_name)