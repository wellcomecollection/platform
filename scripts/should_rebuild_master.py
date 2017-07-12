#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Decide whether we should re-build tests for a project on master.

Exits with code 1 if there are changes that require a rebuild, 0 if not.
"""

import os
import sys

from should_rerun_tests import should_retest_project
import tooling


def should_rebuild_project(changed_files, project):
    """
    Given a set of changed files, return True/False if we should
    rebuild this project.
    """
    if should_retest_project(changed_files, project):
        return True

    elif ('docker/' in changed_files):
        print("*** Changes to the docker directory mean we should rebuild")
        return True

    elif any(f.startswith('scripts/') for f in changed_files):
        print("*** Changes to the scripts directory mean we should rebuild")
        return True

    elif any(f.startswith('.travis') for f in changed_files):
        print("*** Changes to the Travis config mean we should rebuild")
        return True
    elif 'Makefile' in changed_files:
        print("*** Changes to the Makefile mean we should rebuild")
        return True

    return False


if __name__ == '__main__':

    # Travis has an environment variable $TRAVIS_COMMIT_RANGE which tells us
    # the range of commits that this push is testing.  By inspecting its
    # value and passing it to `git diff`, we can determine which files
    # have changed.
    changed_files = tooling.changed_files([os.environ['TRAVIS_COMMIT_RANGE']])

    should_rebuild = should_rebuild_project(
        changed_files=changed_files,
        project=os.environ['PROJECT']
    )
    if should_rebuild:
        sys.exit(1)
    else:
        sys.exit(0)
