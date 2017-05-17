#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Decide whether we should re-build tests for a project on master.

Exits with code 0 if there are changes that require a rebuild, 1 if not.
"""

import os
import subprocess
import sys

from should_rerun_tests import should_retest_project


def modified_files():
    """Returns a set of changed files since the last master build."""
    # Travis has an environment variable $TRAVIS_COMMIT_RANGE which tells us
    # the range of commits that this push is testing.  By inspecting its
    # value and passing it to `git diff`, we can determine which files
    # have changed.
    commit_range = os.environ['TRAVIS_COMMIT_RANGE']

    files = set()
    command = ['git', 'diff', '--name-only', commit_range]
    diff_output = subprocess.check_output(command).decode('ascii')
    for line in diff_output.splitlines():
        filepath = line.strip()
        if filepath:
            files.add(filepath)
    return files


def should_rebuild_project(changed_files, project):
    """
    Given a set of changed files, return True/False if we should
    rebuild this project.
    """
    if should_retest_project(changed_files, project):
        return True

    elif ('Dockerfile' in changed_files) or ('.dockerignore' in changed_files):
        print("*** Changes to Dockerfile mean we should rebuild")
        return True

    return False


if __name__ == '__main__':
    changed_files = modified_files()
    should_rebuild = should_rebuild_project(
        changed_files=changed_files,
        project=os.environ['PROJECT']
    )
    if should_rebuild:
        sys.exit(0)
    else:
        sys.exit(1)
