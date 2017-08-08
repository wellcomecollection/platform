#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
Handles creating or destroying an AWS Batch compute environment

Usage:
  aws_batch_compute.py create <location>
  aws_batch_compute.py destroy <name>
  aws_batch_compute.py -h | --help

Options:
  -h --help                Show this screen.
  create <location>        Location of json description to create
  destroy <name>           Name of the environment to destroy
"""

import json
from time import sleep

import boto3
import docopt
from pprint import pprint


def _await_aws_modify(match_string, op, *args, **kwargs):
    max_attempts = 30
    attempts = 1

    while attempts <= max_attempts:
        try:
            op(*args, **kwargs)
            print('Done.')

            return
        except client.exceptions.ClientException as e:
            if match_string in str(e):

                print('Matched state: "%s" (Retry %s/%s).' % (
                    match_string,
                    attempts,
                    max_attempts)
                )

                attempts += 1
                sleep(5)
            else:
                raise(e)

    raise Exception('Timed out waiting for state change to complete!')


def _attempt_delete(client, description):
    name = description['computeEnvironmentName']
    state = description['state']
    status = description['status']

    print('Attempting to delete %s \n' % name)
    pprint(description)

    if status == 'DELETING' or status == 'DELETED':
        print('%s already %s. Taking no action' % (name, status))
        return
    elif status == 'INVALID':
        raise Exception('%s state is INVALID, cannot delete.')

    if state is not 'DISABLED':
        print('Setting state to DISABLED for %s' % name)
        _await_aws_modify(
            'is being modified',
            _disable_compute_environment,
            client, name
        )

    print('Attempting to delete %s' % name)

    _await_aws_modify(
        'is being modified',
        client.delete_compute_environment,
        computeEnvironment=name
    )


def _disable_compute_environment(client, name):
    client.update_compute_environment(
        computeEnvironment=name,
        state='DISABLED'
    )

def _environment_exists(client, name):
    response = client.describe_compute_environments(
        computeEnvironments=[
            name,
        ]
    )

    if len(response["computeEnvironments"]) > 0:
        return response["computeEnvironments"][0]
    else:
        return None


def _process_already_created_status(description):
    pprint(description)

    name = description['computeEnvironmentName']
    status = description['status']

    if status == 'DELETING':
        raise Exception('%s is DELETING. Error creating compute environment.' % name)
    else:
        print('%s is %s. Taking no action.' % (name, status))


def create_compute_environment(client, requested_description):
    name = requested_description["computeEnvironmentName"]
    description = _environment_exists(client, name)

    if description:
        print('%s already exists!\n' % name)
        _process_already_created_status(description)
    else:
        print('Attempting to create %s', name)
        pprint(requested_description)

        client.create_compute_environment(**requested_description)

        print('Request to create %s accepted.' % name)


def delete_compute_environment(client, name):
    description = _environment_exists(client, name)

    if description:
        _attempt_delete(client, description)
    else:
        print('%s does not exist. Taking no action.' % name)


if __name__ == '__main__':
    args = docopt.docopt(__doc__)
    client = boto3.client('batch')

    if args['create']:
        description = json.loads(open(args['<location>']).read())
        create_compute_environment(client, description)
    elif args['destroy']:
        delete_compute_environment(client, args['<name>'])
