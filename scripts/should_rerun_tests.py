#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Decide whether we should re-run tests for a project on a pull request.

Exits with code 0 if there are changes that require a retest, 1 if not.
"""

import os
import subprocess
import sys


def modified_files(a, b):
    """Returns a set of changed files between ``a`` and ``b``.

    >>> modified_files('HEAD', '^HEAD')
    >>> modified_files('ab27a1f', '8cae231')
    >>> modified_files('master', 'feature-branch')

    """
    files = set()
    command = ['git', 'diff', '--name-only', a, b]
    diff_output = subprocess.check_output(command).decode('ascii')
    for line in diff_output.splitlines():
        filepath = line.strip()
        if filepath:
            files.add(filepath)
    return files


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

    else:
        return False


if __name__ == '__main__':
    changed_files = modified_files('HEAD', 'master')
    should_retest = should_retest_project(
        changed_files=changed_files,
        project=os.environ['PROJECT']
    )
    if should_retest:
        sys.exit(0)
    else:
        sys.exit(1)
