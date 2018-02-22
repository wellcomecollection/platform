#!/usr/bin/env python
# -*- encoding: utf-8
"""
Wrapper script for doing auto-formatting in Travis.

In particular, this script will autoformat Terraform and Scala, then commit
and push the results.  It also lints Python and JSON, but those aren't
auto-formatted (yet).
"""

import os
import sys

from travistooling import changed_files, check_call, git, make


if __name__ == '__main__':
    make('format')

    # https://graysonkoonce.com/getting-the-current-branch-name-during-a-pull-request-in-travis-ci/
    if os.environ['TRAVIS_PULL_REQUEST'] == 'false':
        branch = os.environ['TRAVIS_BRANCH']
    else:
        branch = os.environ['TRAVIS_PULL_REQUEST_BRANCH']

    if changed_files():
        print(
            '*** There were changes from formatting, creating a commit',
            flush=True)

        git('config', 'user.name', 'Travis CI on behalf of Wellcome')
        git('config', 'user.email', 'wellcomedigitalplatform@wellcome.ac.uk')
        git('config', 'core.sshCommand', 'ssh -i id_rsa')

        git(
            'remote', 'add', 'ssh-origin',
            'git@github.com:wellcometrust/platform.git'
        )

        # Unencrypt the SSH key.
        check_call([
            'openssl', 'aes-256-cbc',
            '-K', os.environ['encrypted_83630750896a_key'],
            '-iv', os.environ['encrypted_83630750896a_iv'],
            '-in', '.travis/id_rsa.enc',
            '-out', 'id_rsa', '-d'
        ])
        check_call(['chmod', '400', 'id_rsa'])

        # We checkout the branch before we add the commit, so we don't
        # include the merge commit that Travis makes.
        git('fetch', 'ssh-origin')
        git('checkout', branch)

        git('add', '--verbose', '--all')
        git('commit', '-m', 'Apply auto-formatting rules')
        git('push', 'ssh-origin', 'HEAD:%s' % branch)

        # We exit here to fail the build, so Travis will skip to the next
        # build, which includes the autoformat commit.
        sys.exit(1)
    else:
        print('*** There were no changes from auto-formatting', flush=True)

    make('check-format')
