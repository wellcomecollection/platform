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

import os
import shutil
import subprocess

import docopt

from tooling import (
    write_release_id,
    CURRENT_COMMIT,
    ROOT,
    PLATFORM_ENV
)


DEFAULT_VERSION = '0.0.1'


if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    # Read arguments from docopt
    project = args['--project']
    version = args['--version'] or DEFAULT_VERSION
    build_env = args['--env'] or PLATFORM_ENV

    print('*** Building sbt Docker image for %s' % project)

    # Construct the release ID and the tag
    release_id = '%s-%s_%s' % (version, CURRENT_COMMIT, build_env)
    tag = '%s:%s' % (project, release_id)
    print('*** Image will be tagged %s' % tag)

    print('*** Building the Scala binaries')
    subprocess.check_call(['sbt', 'project %s' % project, 'stage'])

    source_target = os.path.join(ROOT, project, 'target', 'universal', 'stage')
    docker_root = os.path.join(ROOT, 'docker', 'scala_service')
    dest_target = os.path.join(docker_root, 'target', project)

    print('*** Copying build artefacts to %s from %s' % (dest_target, source_target))

    shutil.rmtree(dest_target, ignore_errors=True)
    shutil.copytree(source_target, dest_target)

    print('*** Building the new Docker image')
    print('*** Dockerfile is at %s' % docker_root)
    subprocess.check_call([
        'docker', 'build',
        '--file', os.path.join(docker_root, 'Dockerfile'),
        '--tag', tag,
        '--build-arg', 'project=%s' % project,
        docker_root
    ])

    print('*** Saving the release ID to .releases')
    write_release_id(project=project, release_id=release_id)
