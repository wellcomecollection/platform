#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Builds a Docker image for use in CI or a local Make task.

This script does _not_ run inside a container, so it has to work on any
system.  This means:
 1. No third-party Python libraries!
 2. Python 2 and Python 3 compatible.

"""

import argparse
import os
import subprocess

from tooling import mkdir_p, ROOT


def parse_args():
    parser = argparse.ArgumentParser(
        description='Build a Docker image for use in CI or a local Make task.'
    )
    parser.add_argument(
        '--project', required=True,
        help='Name of the Docker project to build'
    )
    parser.add_argument(
        '--dir', required=True,
        help='Directory containing the Dockerfile'
    )
    parser.add_argument(
        '--file', required=False,
        help='Path to the Dockerfile'
    )
    return parser.parse_args()


if __name__ == '__main__':
    args = parse_args()
    project = args.project
    directory = args.dir

    command = ['docker', 'build', '--tag', project, directory]
    if args.file:
        command.extend(['--file', args.file])

    subprocess.check_call(command, cwd=ROOT)
    mkdir_p(os.path.join(ROOT, '.docker'))
    open(os.path.join(ROOT, '.docker', project), 'w')
