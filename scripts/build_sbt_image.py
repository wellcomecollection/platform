#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
Build a Docker image for one of our sbt applications.

Usage:
  build_sbt_image.py --project=<PROJECT> [--version=<VERSION>] [--env=<BUILD_ENV>]
  build_sbt_image.py -h | --help

Options:
  -h --help                  Show this screen.
  --project=<PROJECT>        Name of the sbt project (e.g. api, transformer)
  --version=<VERSION>        Version to use in the release ID
  --env=<BUILD_ENV>          Build environment (dev, prod, etc.)

"""

import subprocess

import docker
import docopt

from tooling import write_release_id, CURRENT_COMMIT, ROOT


DEFAULT_VERSION = '0.0.1'

DEFAULT_BUILD_ENV = 'dev'


if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    # Read arguments from docopt
    project = args['--project']
    version = args['--version'] or DEFAULT_VERSION
    build_env = args['--env'] or DEFAULT_BUILD_ENV

    print(f'*** Building sbt Docker image for {project}')

    # Construct the release ID and the tag
    release_id = f'{version}-{CURRENT_COMMIT}_{build_env}'
    tag = f'{project}:{release_id}'
    print(f'*** Image will be tagged {tag}')

    print(f'*** Building the Scala binaries')
    subprocess.check_call(['sbt', f'project {project}', 'stage'])

    # Construct the path to required build artifacts
    target = f'{project}/target/universal/stage'
    print(f'*** Build target is {target}')

    print('*** Building the new Docker image')
    dockerfile = f'{ROOT}/docker/scala_service'
    print(f'*** Dockerfile is at {dockerfile}')
    client = docker.from_env()
    client.images.build(path=dockerfile, buildargs={'project': project, 'target': target}, tag=tag)

    print('*** Saving the release ID to .releases')
    write_release_id(project=project, release_id=release_id)
