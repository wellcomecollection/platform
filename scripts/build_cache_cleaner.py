#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
Build the Docker image for our cache cleaner.

Usage:
  build_cache_cleaner.py
  build_cache_cleaner.py -h | --help

Options:
  -h --help         Show this screen.
"""

import os
import sys

import docopt
import docker

from tooling import write_release_id, CURRENT_COMMIT, ROOT


if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    print('*** Building Docker image for cache cleaner')

    release_id = CURRENT_COMMIT
    tag = 'cache_cleaner:%s' % release_id
    print('*** Image will be tagged %s' % tag)

    print('*** Building the new Docker image')
    client = docker.from_env()
    client.images.build(
        path=os.path.join(ROOT, 'cache_cleaner'),
        tag=tag
    )

    print('*** Saving the release ID to .releases')
    write_release_id(project='cache_cleaner', release_id=release_id)
