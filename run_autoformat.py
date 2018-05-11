# -*- encoding: utf-8
"""
This script does autoformatting in Travis CI on pull requests.

In particular, it runs the 'make format' task, and if there are any changes,
it pushes a new commit to your pull request and aborts the current build.
"""

import sys

from travistooling import branch_name, get_changed_paths, git, make


if __name__ == '__main__':

    # First get information about the currently running patch.
    # In particular, we want to know which files have actually changed.
    travis_event_type = os.environ['TRAVIS_EVENT_TYPE']

    if travis_event_type == 'pull_request':
        changed_paths = get_changed_paths('HEAD', 'master')
    else:
        git('fetch', 'origin')
        changed_paths = get_changed_paths(os.environ['TRAVIS_COMMIT_RANGE'])

    # Then run the 'format' tasks.  These are any tasks which might edit
    # the code, and for which we might push changes.
    extension_to_format_task = [
        ('.tf', 'format-terraform'),
        ('.scala', 'format-scala'),
        ('.py', 'format-python'),
        ('.json', 'format-json'),
    ]

    for extension, format_task in extension_to_format_task:
        relevant_paths = [f for f in changed_paths if f.endswith(extension)]
        if relevant_paths:
            print('*** Running %s for the following paths:' % format_task)
            for p in relevant_paths:
                print(' - %s' % p)
            make(format_task)
        else:
            print(
                '*** Skipping %s as there are no affected files' % format_task)

    # If there are any changes, push to GitHub immediately and fail the
    # build.  This will abort the remaining jobs, and trigger a new build
    # with the reformatted code.
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
        git('push', 'ssh-origin', 'HEAD:%s' % branch_name())

        # We exit here to fail the build, so Travis will skip to the next
        # build, which includes the autoformat commit.
        sys.exit(1)
    else:
        print('*** There were no changes from auto-formatting')

    # Finally, run the 'lint' tasks.  A failure in these tasks requires
    # manual intervention, so we run them last to get any automatic fixes
    # out of the way.
    extension_to_lint_task = [
        ('.py', 'lint-python'),
        ('.ttl', 'lint-ontologies'),
    ]

    for extension, lint_task in extension_to_lint_task:
        if any(f.endswith(extension) in changed_paths):
            make(lint_task)
