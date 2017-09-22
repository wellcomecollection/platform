#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Run a Docker image used by a Make task.

This script does _not_ run inside a container, so it has to work on any
system.  This means:
 1. No third-party Python libraries!
 2. Python 2 and Python 3 compatible.

Arguments after the two flags are passed directly to ``docker run``.

"""

import argparse
import os
import subprocess

from tooling import ROOT


def _aws_credentials_args():
    """
    Returns the arguments to add to ``docker run`` for sharing AWS credentials
    with the running container.
    """
    try:
        print('*** Trying environment variables for AWS config...')
        return [
            '--env', 'AWS_ACCESS_KEY_ID=%s' % os.environ['AWS_SECRET_KEY_ID'],
            '--env', 'AWS_SECRET_ACCESS_KEY=%s' % os.environ['AWS_SECRET_ACCESS_KEY'],
            '--env', 'AWS_REGION=%s' % os.environ['AWS_REGION'],
            '--env', 'AWS_DEFAULT_REGION=%s' % os.environ['AWS_DEFAULT_REGION'],
        ]
    except KeyError as err:
        print('*** Missing environment variable (%r), using ~/.aws' % err)
        aws_path = os.path.join(os.environ['HOME'], '.aws')
        return ['--volume', '%s:/root/.aws' % aws_path]


def parse_args():
    parser = argparse.ArgumentParser(
        description='Run a Docker image used by a Make task.'
    )
    parser.add_argument(
        '--aws', dest='share_aws_creds', action='store_const', const=True,
        help='Whether to share AWS credentials with the running container'
    )
    parser.add_argument(
        '--dind', dest='docker_in_docker', action='store_const', const=True,
        help='Whether to allow this container to run Docker'
    )
    return parser.parse_known_args()


if __name__ == '__main__':
    namespace, additional_args = parse_args()

    cmd = ['docker', 'run']

    if namespace.share_aws_creds:
        cmd += _aws_credentials_args()

    if namespace.docker_in_docker:
        cmd += ['--volume', '%s:/repo' % ROOT]
        cmd += ['--volume', '/var/run/docker.sock:/var/run/docker.sock']

    if additional_args[0] == '--':
        additional_args = additional_args[1:]
    cmd += additional_args

    subprocess.check_call(cmd)
