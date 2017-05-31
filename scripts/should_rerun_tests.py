#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Decide whether we should re-run tests for a project on a pull request.

Exits with code 1 if there are changes that require a retest, 0 if not.
"""

import os
import sys

import tooling


def should_retest_project(changed_files, project):
    """
    Given a set of changed files, return True/False if we should re-run
    the tests for this project.
    """
    if 'build.sbt' in changed_files:
        print("*** Changes to build.sbt mean we should rebuild")
        return True

    elif any(f.startswith(('common/', 'project/')) for f in changed_files):
        print("*** Changes to common/project dirs mean we should rebuild")
        return True

    elif any(f.startswith('%s/' % project) for f in changed_files):
        print("*** Changes to the project dir mean we should rebuild")
        return True

    elif 'scripts/run_tests.sh' in changed_files:
        print("*** Changes to the test runner mean we should retest")
        return True

    else:
        return False


if __name__ == '__main__':
    changed_files = tooling.changed_files(['HEAD', 'master'])
    should_retest = should_retest_project(
        changed_files=changed_files,
        project=os.environ['PROJECT']
    )
    if should_retest:
        sys.exit(1)
    else:
        sys.exit(0)
