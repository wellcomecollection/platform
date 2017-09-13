#!/usr/bin/env python
# -*- encoding: utf-8 -*-

from __future__ import print_function

import os
import subprocess
import sys


# Root of the Git repository
ROOT = subprocess.check_output([
    'git', 'rev-parse', '--show-toplevel']).decode('ascii').strip()


def changed_files(*args):
    """
    Returns a set of changed files in a given commit range.

    :param commit_range: Arguments to pass to ``git diff``.
    """
    files = set()
    command = ['git', 'diff', '--name-only'] + list(args)
    diff_output = subprocess.check_output(command).decode('ascii')
    for line in diff_output.splitlines():
        filepath = line.strip()
        if filepath:
            files.add(filepath)
    return files


def are_there_job_relevant_changes(changed_files, task):
    """
    Given a list of changed files and a Make task, are there any reasons
    why the output of this task might have changed?

    e.g. if the loris/ directory changes, we should re-run tasks associated
         with Loris.

    Returns a list of reasons why the task should be run.
    """
    reasons = []

    if task == 'check-format':
        reasons.append('Linting/formatting tasks always run')

    if 'Makefile' in changed_files:
        reasons.append('Changes to the Makefile')

    if any(f.startswith('.travis') for f in changed_files):
        reasons.append('Changes to the Travis config')

    if any(f.startswith(('scripts/', 'builds/')) for f in changed_files):
        reasons.append('Changes to the build scripts')

    if task.startswith('sbt-'):
        reasons.extend(_are_there_sbt_relevant_changes(changed_files, task))

    docker_images = os.listdir('docker')
    if any(task.startswith(d) for d in docker_images):
        reasons.extend(_are_there_docker_relevant_changes(changed_files, task))

    for project in os.listdir(ROOT):
        if task.startswith(project):
            if any(f.startswith('%s/' % project) for f in changed_files):
                reasons.append('Changes to %s/' % project)

    return reasons


def _are_there_sbt_relevant_changes(changed_files, task):
    """
    Return a list of reasons we might want to re-run an sbt task.
    """
    reasons = []

    if 'build.sbt' in changed_files:
        reasons.append('Changes to build.sbt')

    sbt_project = task.split('-')[-1]
    for dirname in ['common', 'project', sbt_project]:
        if any(f.startswith('%s/' % dirname) for f in changed_files):
            reasons.append('Changes to %s/' % dirname)

    return reasons


def _are_there_docker_relevant_changes(changed_files, task):
    """
    Return a list of reasons we might want to re-run a task in the
    docker directory.
    """
    image = task.split('-')[0]
    if any(f.startswith('docker/%s/' % image) for f in changed_files):
        return ['Changes to docker/%s' % image]


def make_decision(changed_files, task, action):
    reasons = are_there_job_relevant_changes(
        changed_files=changed_files, task=task
    )
    if reasons:
        print('*** Reasons to %s for this change:' % action)
        for r in reasons:
            print('***   - %s' % r)
        sys.exit(1)
    else:
        print('*** No reasons to %s for this change!' % action)
        sys.exit(0)
