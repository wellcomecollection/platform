#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
Builds a docker image for a simple project

Usage:
  build_docker_image.py --project=<name> [--variant=<variant>]
  build_docker_image.py -h | --help

Options:
  -h --help                Show this screen.
  --project=<project>      Name of the project (e.g. api, loris).  Assumes
                           there's a folder containing a Dockerfile with the
                           same name.
  --variant=<variant>      The optional variant of this project (e.g. nginx).
                           Relies on the Dockerfile accepting a variant ENV.
"""

import os
import sys

import docopt
import docker

from tooling import write_release_id, CURRENT_COMMIT, ROOT


if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    variant = args['--variant']
    project = args['--project']

    print('*** Building image for %s' % project)

    release_id = CURRENT_COMMIT

    if variant is not None:
        tag = '%s_%s:%s' % (project, variant, release_id)
    else:
        tag = '%s:%s' % (project, release_id)

    print('*** Image will be tagged %s' % tag)

    print('*** Building the new image')
    client = docker.from_env()
    client.images.build(
        path=os.path.join(ROOT, 'docker', project),
        buildargs={'variant': variant},
        tag=tag
    )

    print('*** Saving the release ID to .releases')
    write_release_id(project=project, release_id=release_id)
