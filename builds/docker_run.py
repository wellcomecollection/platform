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
import sys

# Root of the Git repository
ROOT = subprocess.check_output([
    'git', 'rev-parse', '--show-toplevel']).decode('ascii').strip()


def _aws_credentials_args():
    """
    Returns the arguments to add to ``docker run`` for sharing AWS credentials
    with the running container.
    """
    # THE AWS_PROFILE environment allows you to run operations in a
    # non-default profile.  If you have multiple profiles in your ~/.aws
    # config, use this variable to choose a non-default profile.  For example:
    #
    #   AWS_PROFILE=platform ./docker_run.py --aws -- ...
    #
    # For details:
    # https://docs.aws.amazon.com/cli/latest/userguide/cli-multiple-profiles.html
    try:
        cmd = ['--env', 'AWS_PROFILE=%s' % os.environ['AWS_PROFILE']]
    except KeyError:
        cmd = []

    if 'AWS_ACCESS_KEY_ID' in os.environ:
        print('*** Trying environment variables for AWS config...')
        cmd.extend([
            '--env', 'AWS_ACCESS_KEY_ID=%s' % os.environ.get('AWS_ACCESS_KEY_ID', ''),
            '--env', 'AWS_SECRET_ACCESS_KEY=%s' % os.environ.get('AWS_SECRET_ACCESS_KEY', ''),
            '--env', 'AWS_REGION=%s' % os.environ.get('AWS_REGION', ''),
            '--env', 'AWS_DEFAULT_REGION=%s' % os.environ.get('AWS_DEFAULT_REGION', ''),
        ])
    else:
        print('*** Missing environment variable, using ~/.aws')
        aws_path = os.path.join(os.environ['HOME'], '.aws')
        cmd.extend(['--volume', '%s:/root/.aws' % aws_path])

    return cmd


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
    parser.add_argument(
        '--sbt', dest='share_sbt_dirs', action='store_const', const=True,
        help='Whether to share sbt directories with the running container'
    )
    parser.add_argument(
        '--root', dest='expose_host_root_folder', action='store_const', const=True,
        help='Whether to expose the name of the root folder of the repository in the host'
    )
    return parser.parse_known_args()


if __name__ == '__main__':
    namespace, additional_args = parse_args()

    cmd = ['docker', 'run', '--tty', '--rm']

    if namespace.share_aws_creds:
        cmd += _aws_credentials_args()

    if namespace.docker_in_docker:
        cmd += ['--volume', '%s:/repo' % ROOT]
        cmd += ['--volume', '/var/run/docker.sock:/var/run/docker.sock']

    if namespace.share_sbt_dirs:
        cmd += ['--volume', '%s/.sbt:/root/.sbt' % os.environ['HOME']]
        cmd += ['--volume', '%s/.ivy2:/root/.ivy2' % os.environ['HOME']]

    if namespace.expose_host_root_folder:
        cmd += ['-e', 'ROOT=%s' % ROOT]

    if additional_args[0] == '--':
        additional_args = additional_args[1:]
    cmd += additional_args

    try:
        print('*** Running %r' % ' '.join(cmd))
        rc = subprocess.call(cmd)
        if rc != 0:
            sys.exit(rc)
    except KeyboardInterrupt:
        sys.exit(1)
