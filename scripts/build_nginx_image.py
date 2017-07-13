#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
Build an image for one of our nginx proxies.

Usage:
  build_nginx_image.py --variant=<variant>
  build_nginx_image.py -h | --help

Options:
  -h --help                Show this screen.
  --variant=<variant>      Name of the variant (e.g. api, loris, services).

"""

import os
import sys

import docopt
import docker

from tooling import write_release_id, CURRENT_COMMIT, ROOT


if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    variant = args['--variant']
    print('*** Building nginx Docker image for %s' % variant)

    conf_file = os.path.join(ROOT, 'docker', 'nginx', '%s.nginx.conf' % variant)
    if not os.path.exists(conf_file):
        sys.exit('*** Unable to find %s!' % conf_file)

    release_id = CURRENT_COMMIT
    tag = 'nginx_%s:%s' % (variant, release_id)
    print('*** Image will be tagged %s' % tag)

    print('*** Building the new Docker image')
    client = docker.from_env()
    client.images.build(
        path=os.path.join(ROOT, 'docker', 'nginx'),
        buildargs={'conf_file': '%s.nginx.conf' % variant},
        tag=tag
    )

    print('*** Saving the release ID to .releases')
    write_release_id(project='nginx_%s' % variant, release_id=release_id)
