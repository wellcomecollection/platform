#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
Builds a Docker image for a project.

Usage:
  build_docker_image.py --project=<PROJECT> [--file=<FILE>] [--variant=<VARIANT>]
  build_docker_image.py -h | --help

Options:
  -h --help                Show this screen
  --project=<PROJECT>      Name of the Docker image to build
  --file=<FILE>            Path to the Dockerfile (if not in docker/<PROJECT>)
  --variant=<VARIANT>      The optional variant of this project (e.g. nginx)
                           Relies on the Dockerfile accepting a variant ARG

"""

import os
import subprocess

import docopt

from tooling import write_release_id, CURRENT_COMMIT, ROOT


if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    project = args['--project']
    variant = args['--variant']
    if args['--file']:
        dockerfile = os.path.join(ROOT, args['--file'])
    else:
        dockerfile = os.path.join(ROOT, 'docker', project, 'Dockerfile')

    print('*** Building image for %s' % project)

    release_id = CURRENT_COMMIT

    if variant is not None:
        tag = '%s_%s:%s' % (project, variant, release_id)
        release_name = '%s_%s' % (project, variant)
    else:
        tag = '%s:%s' % (project, release_id)
        release_name = project

    print('*** Image will be tagged %s' % tag)

    print('*** Building the new image')

    cmd = ['docker', 'build', '--file', dockerfile, '--tag', tag]
    if variant is not None:
        cmd.extend(['--build-arg', 'variant=%s' % variant])
    cmd.append(os.path.dirname(dockerfile))

    subprocess.check_call(cmd)

    print('*** Saving the release ID to .releases')
    write_release_id(project=release_name, release_id=release_id)
