#!/usr/bin/env python
# -*- encoding: utf-8 -*-

from __future__ import print_function

import os
import re
import subprocess
import sys


# Root of the Git repository
ROOT = subprocess.check_output([
    'git', 'rev-parse', '--show-toplevel']).decode('ascii').strip()


def fprint(*args, **kwargs):
    if kwargs.get('file') == sys.stderr:
        kwargs['file'] = sys.stderr.flush()
    else:
        kwargs['file'] = sys.stdout.flush()
    print(*args, **kwargs)


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

    if 'format' in task:
        reasons.append('Linting/formatting tasks always run')

    if 'Makefile' in changed_files:
        reasons.append('Changes to the Makefile')

    if any(f.startswith('.travis') for f in changed_files):
        reasons.append('Changes to the Travis config')

    if any(f.startswith('builds/') for f in changed_files):
        reasons.append('Changes to the build scripts')

    if any(task.startswith(p) for p in _sbt_projects()):
        reasons.extend(_are_there_sbt_relevant_changes(changed_files, task))

    for project in os.listdir(ROOT):
        if task.startswith(project):
            if any(f.startswith('%s/' % project) for f in changed_files):
                reasons.append('Changes to %s/' % project)

    return reasons


def _sbt_projects():
    """Returns a list of sbt project names."""
    for line in open(os.path.join(ROOT, 'build.sbt')):
        m = re.match(r'lazy val (?P<project>[a-z_]+)', line)
        if (m is not None) and (m.group('project') != 'root'):
            yield m.group('project')


def _are_there_sbt_relevant_changes(changed_files, task):
    """
    Return a list of reasons we might want to re-run an sbt task.
    """
    reasons = []

    if 'build.sbt' in changed_files:
        reasons.append('Changes to build.sbt')

    sbt_project = task.split('-')[-1]
    for dirname in ['common', 'project', sbt_project]:
        if any(dirname in f for f in changed_files):
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
    else:
        return []


def make_decision(changed_files, task, action):
    reasons = are_there_job_relevant_changes(
        changed_files=changed_files, task=task
    )
    if reasons:
        print('*** Reasons to %s for this change:' % action)
        for r in reasons:
            print('***   - %s' % r)
        return True
    else:
        print('*** No reasons to %s for this change!' % action)
        return False


def make(task):
    print('*** Running make %s' % task)
    subprocess.check_call(['make', task])


def git(*args):
    print('*** Running git %s' % ' '.join(args))
    subprocess.check_call(['git'] + list(args))


def rreplace(string, old, new, count=None):
    """
    Replace ``old`` with ``new``, starting from the right.
    """
    # Split on the occurrences of ``old``, starting from the right
    parts = string.rsplit(old, count)

    # Then join back together with ``new``
    return new.join(parts)
