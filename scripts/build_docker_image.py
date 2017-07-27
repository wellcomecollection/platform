#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
Builds a docker image for a simple project

Usage:
  build_docker_image.py --project=<name>
  build_docker_image.py -h | --help

Options:
  -h --help                Show this screen.
  --project=<project>      Name of the project (e.g. api, loris).  Assumes
                           there's a folder containing a Dockerfile with the
                           same name.
"""

import os
import sys

import docopt
import docker

from tooling import write_release_id, CURRENT_COMMIT, ROOT


if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    project = args['--project']

    print('*** Building Docker image for %s' % project)

    release_id = CURRENT_COMMIT
    tag = f'{project}:{release_id}'
    print('*** Image will be tagged %s' % tag)

    print('*** Building the new Docker image')
    client = docker.from_env()
    client.images.build(
        path=os.path.join(ROOT, 'docker', project),
        tag=tag
    )

    print('*** Saving the release ID to .releases')
    write_release_id(project=project, release_id=release_id)
