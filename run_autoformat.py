# -*- encoding: utf-8
"""
This script does autoformatting in Travis CI on pull requests.

In particular, it runs the 'make format' task, and if there are any changes,
it pushes a new commit to your pull request and aborts the current build.
"""

import os
import subprocess
import sys


def branch_name():
    """Return the name of the branch under test."""
    # See https://graysonkoonce.com/getting-the-current-branch-name-during-a-pull-request-in-travis-ci/
    if os.environ["TRAVIS_PULL_REQUEST"] == "false":
        return os.environ["TRAVIS_BRANCH"]
    else:
        return os.environ["TRAVIS_PULL_REQUEST_BRANCH"]


def check_call(cmd):
    """
    A wrapped version of subprocess.check_call() that doesn't print a
    traceback if the command errors.
    """
    print("*** Running %r" % " ".join(cmd))
    try:
        return subprocess.check_call(cmd)
    except subprocess.CalledProcessError as err:
        print(err)
        sys.exit(err.returncode)


def git(*args):
    """Run a Git command and return its output."""
    cmd = ["git"] + list(args)
    try:
        return subprocess.check_output(cmd).decode("utf8").strip()
    except subprocess.CalledProcessError as err:
        print(err)
        sys.exit(err.returncode)


def make(*args):
    """Run a Make command, and check it completes successfully."""
    check_call(["make"] + list(args))


def get_changed_paths(*args):
    """
    Returns a set of changed paths in a given commit range.

    :param commit_range: Arguments to pass to ``git diff``.
    """
    diff_output = git("diff", "--name-only", *args)

    return set([line.strip() for line in diff_output.splitlines()])


if __name__ == "__main__":

    make("format")

    # If there are any changes, push to GitHub immediately and fail the
    # build.  This will abort the remaining jobs, and trigger a new build
    # with the reformatted code.
    if get_changed_paths():
        print("*** There were changes from formatting, creating a commit")

        git("config", "user.name", "Travis CI on behalf of Wellcome")
        git("config", "user.email", "wellcomedigitalplatform@wellcome.ac.uk")
        git("config", "core.sshCommand", "ssh -i id_rsa")

        git(
            "remote",
            "add",
            "ssh-origin",
            "git@github.com:wellcometrust/storage-service.git",
        )

        # We checkout the branch before we add the commit, so we don't
        # include the merge commit that Travis makes.
        git("fetch", "ssh-origin")
        git("checkout", branch_name())

        git("add", "--verbose", "--update")
        git("commit", "-m", "Apply auto-formatting rules")
        git("push", "ssh-origin", "HEAD:%s" % branch_name())

        # We exit here to fail the build, so Travis will skip to the next
        # build, which includes the autoformat commit.
        sys.exit(1)
    else:
        print("*** There were no changes from auto-formatting")

    # Run the 'lint' tasks.  A failure in these tasks requires
    # manual intervention, so we run them second to get any automatic fixes
    # out of the way.
    make("lint")
