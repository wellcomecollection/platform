#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Decide whether we should re-run tests for a project on a pull request.

Exits with code 1 if there are changes that require a retest, 0 if not.
"""

from __future__ import print_function

import os
import sys

import tooling


class ShouldRebuild(Exception):
    pass


def should_run_tests(changed_files, task):
    """
    Given a set of changed files, check if we need to run tests.
    """
    if task in 'check-format':
        raise ShouldRebuild('Linting/formatting tasks should always run')

    if 'Makefile' in changed_files:
        raise ShouldRebuild(
            'Changes to the Makefile always trigger a full run'
        )

    if any(f.startswith('.travis') for f in changed_files):
        raise ShouldRebuild(
            'Changes to the Travis config always trigger a full run'
        )

    if any(f.startswith('scripts/') for f in changed_files):
        raise ShouldRebuild(
            'Changes to the build scripts always trigger a full run'
        )

    if task.startswith('sbt-'):
        _should_run_tests_sbt(changed_files=changed_files, task=task)

    docker_images = os.listdir('docker')
    if any(task.startswith('d') for d in docker_images):
        _should_run_tests_docker(changed_files=changed_files, task=task)

    if (
        task.startswith('miro_adapter') and
        any(c.startswith('miro_adapter') for c in changed_files)
    ):
        raise ShouldRebuild(
            'Changes to the miro_adapter directory trigger a full run'
        )


def _should_run_tests_sbt(changed_files, task):
    """
    Given an sbt task, check if we need to run tests.
    """
    if 'build.sbt' in changed_files:
        raise ShouldRebuild('Changes to build.sbt always trigger a full run')

    if any(f.startswith(('common/', 'project/')) for f in changed_files):
        raise ShouldRebuild(
            'Changes to common/project dirs mean we should rebuild'
        )

    sbt_project = task.split('-')[-1]
    if any(f.startswith('%s/' % sbt_project) for f in changed_files):
        raise ShouldRebuild(
            'Changes to the %s dir mean we should rebuild' % sbt_project
        )


def _should_run_tests_docker(changed_files, task):
    """
    Given a Docker task, check if we need to run tests.
    """
    image = task.split('-')[0]
    if any(f.startswith('docker/%s/' % image) for f in changed_files):
        raise ShouldRebuild(
            'Changes to the docker/%s dir mean we should rebuild' % image
        )


if __name__ == '__main__':
    changed_files = tooling.changed_files(['HEAD', 'master'])
    task = os.environ['TASK']

    try:
        should_run_tests(changed_files=changed_files, task=task)
    except ShouldRebuild as err:
        print('*** %s' % err, file=sys.stderr)
        sys.exit(1)
    else:
        sys.exit(0)
