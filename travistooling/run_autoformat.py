# -*- encoding: utf-8
"""
This script does autoformatting in Travis CI on pull requests.

In particular, it runs the 'make format' task, and if there are any changes,
it pushes a new commit to your pull request and aborts the current build.
"""

import sys

from travistooling.git_utils import get_changed_paths, git, make
from travistooling.make_utils import make
from travistooling.travisenv import branch_name


if __name__ == '__main__':
    make('format')

    if get_changed_paths():
        print('*** There were changes from formatting, creating a commit')

        git('config', 'user.name', 'Travis CI on behalf of Wellcome')
        git('config', 'user.email', 'wellcomedigitalplatform@wellcome.ac.uk')
        git('config', 'core.sshCommand', 'ssh -i secrets/id_rsa')

        git(
            'remote', 'add', 'ssh-origin',
            'git@github.com:wellcometrust/platform.git'
        )

        # We checkout the branch before we add the commit, so we don't
        # include the merge commit that Travis makes.
        git('fetch', 'ssh-origin')
        git('checkout', branch_name())

        git('add', '--verbose', '--all')
        git('commit', '-m', 'Apply auto-formatting rules')
        git('push', 'ssh-origin', 'HEAD:%s' % branch)

        # We exit here to fail the build, so Travis will skip to the next
        # build, which includes the autoformat commit.
        sys.exit(1)
    else:
        print('*** There were no changes from auto-formatting')

    make('check-format')
