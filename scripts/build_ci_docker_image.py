#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Builds a Docker image for use in CI or a local Make task.

This script has to work with as few deps installed as possible, so:
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
    return parser.parse_args()


if __name__ == '__main__':
    args = parse_args()
    project = args.project

    subprocess.check_call(
        ['docker', 'build', './docker/%s' % project, '--tag', project],
        cwd=ROOT
    )
    mkdir_p(os.path.join(ROOT, '.docker'))
    open(os.path.join(ROOT, '.docker', project), 'w')
