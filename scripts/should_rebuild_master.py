#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Decide whether we should re-build tests for a project on master.

Exits with code 1 if there are changes that require a rebuild, 0 if not.
"""

from __future__ import print_function

import os
import sys

from should_rerun_tests import should_run_tests, ShouldRebuild
import tooling


def should_run_deploy(changed_files, task):
    """
    Given a set of changed files, check if we need to run tests.
    """
    should_run_tests(changed_files=changed_files, task=task)

    if (
        task.startswith('sbt-') and
        any(f.startswith('docker/scala_service') in changed_files
    ):
        raise ShouldRebuild(
            'Changes to docker/scala_service mean we should deploy'
        )


if __name__ == '__main__':

    # Travis has an environment variable $TRAVIS_COMMIT_RANGE which tells us
    # the range of commits that this push is testing.  By inspecting its
    # value and passing it to `git diff`, we can determine which files
    # have changed.
    changed_files = tooling.changed_files([os.environ['TRAVIS_COMMIT_RANGE']])
    task = os.environ['TASK']

    try:
        should_run_deploy(changed_files=changed_files, task=task)
    except ShouldRebuild as err:
        print('*** %s' % err, file=sys.stderr)
        sys.exit(1)
    else:
        sys.exit(0)
