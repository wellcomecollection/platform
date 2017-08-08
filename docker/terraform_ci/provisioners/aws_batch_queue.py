#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
Handles creating or destroying an AWS Batch compute environment

Usage:
  aws_batch_queue.py create <location>
  aws_batch_queue.py -h | --help

Options:
  -h --help                Show this screen.
  create <location>        Location of json description to create
"""

import json

import boto3
import docopt
from pprint import pprint


def _process_already_created_status(description):
    pprint(description)

    name = description['jobQueueName']
    status = description['status']

    if status == 'DELETING':
        raise Exception('%s is DELETING. Error creating job queue.' % name)
    else:
        print('%s is %s. Taking no action.' % (name, status))


def _queue_exists(client, name):
    response = client.describe_job_queues(
        jobQueues=[
            name,
        ]
    )

    if len(response['jobQueues']) > 0:
        return response['jobQueues'][0]
    else:
        return None


def create_batch_queue(client, requested_description):
    name = requested_description['jobQueueName']
    description = _queue_exists(client, name)

    if description:
        print('%s already exists!\n' % name)
        _process_already_created_status(description)
    else:
        print('Attempting to create %s', name)
        pprint(requested_description)

        client.create_job_queue(**requested_description)

        print('Request to create %s accepted.' % name)


if __name__ == '__main__':
    args = docopt.docopt(__doc__)
    client = boto3.client('batch')

    if args['create']:
        description = json.loads(open(args['<location>']).read())
        create_batch_queue(client, description)
