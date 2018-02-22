#!/usr/bin/env python
# -*- encoding: utf-8 -*-

from __future__ import print_function

import os
import subprocess
import sys


# Root of the Git repository
ROOT = subprocess.check_output([
    'git', 'rev-parse', '--show-toplevel']).decode('ascii').strip()


def check_call(command):
    """
    A wrapped version of subprocess.check_call that doesn't print a traceback
    if the command fails.
    """
    print('*** Running %r' % ' '.join(command), flush=True)
    rc = subprocess.call(command)
    if rc != 0:
        sys.exit(rc)


def fprint(*args, **kwargs):
    kwargs['flush'] = True
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


def affects_tests(path, task):
    """Does this file have any effect on test outcomes?"""
    # Nothing reads our Markdown files in tests, so we can ignore their
    # effect here.
    if path.endswith(('.md', '.png', '.graffle')) or path == 'LICENSE':
        print(
            "~~~ %s is ignored because it's not a file type we care about" %
            path)
        return False

    # The ``misc`` folder is never used anywhere, so we can ignore it.
    if path.startswith('misc/'):
        print("~~~ %s is ignored because it's in the misc dir" % path)
        return False

    # We do lint the JSON and TTL files in the ontologies directory when
    # running in CI, but not in any of the code tests.
    #
    # Since linting already happens unconditionally, we can ignore changes
    # to this directory as well.
    if path.startswith('ontologies/'):
        print("~~~ %s is ignored because it's in the ontologies dir" % path)
        return False

    # Nothing in Travis except the ``travis-format`` task (which always runs)
    # interacts with our Terraform files.
    if path.endswith('.tf'):
        print("~~~ %s is ignored because it's a Terraform file" % path)
        return False

    # For each task, which directories only have an effect on this task?
    #
    # For example, changes to the ``loris`` directory only have an effect
    # on the ``loris`` task; in any other task changes to that directory
    # can be ignored.
    task_specific_directories = {
        'loris': ['loris'],
        'id_minter': ['catalogue_pipeline/id_minter'],
        'ingestor': ['catalogue_pipeline/ingestor'],
        'transformer': ['catalogue_pipeline/transformer'],
        'api': ['catalogue_api'],
        'monitoring': ['monitoring'],
        'shared_infra': ['shared_infra'],
        'nginx': ['nginx'],

        'reindexer_worker': ['reindexer/reindexer_worker'],
        'reindex_job_creator': ['reindexer/reindex_job_creator'],
        'complete_reindex': ['reindexer/complete_reindex'],
        'reindex_shard_generator': ['reindexer/reindex_shard_generator'],
        'resharder': ['reindexer/resharder'],

        's3_demultiplexer': ['sierra_adapter/s3_demultiplexer'],
        'sierra_window_generator': ['sierra_adapter/sierra_window_generator'],
        'sierra_reader': ['sierra_adapter/sierra_reader'],
        'sierra_items_to_dynamo': ['sierra_adapter/sierra_items_to_dynamo'],
        'sierra_bib_merger': ['sierra_adapter/sierra_bib_merger'],
        'sierra_item_merger': ['sierra_adapter/sierra_item_merger'],
    }

    # If we have a change to a file which is specific to a particular task,
    # but we're *not* in that task, this change is unimportant.
    for prefix, directories in task_specific_directories.items():
        if not task.startswith(prefix) and path.startswith(tuple(directories)):
            print('~~~ Ignoring %s; it only affects %s tests' % (path, task))
            return False

        if task.startswith(prefix) and path.startswith(tuple(directories)):
            print("+++ %s is definitely part of the %s tests" % (path, task))
            return True

    # A number of Sierra-related tasks share code/Makefiles in the
    # sierra_adapter directory.  If we're definitely in a project which
    # has nothing to do with Sierra, we can ignore changes in this dir.
    sierra_free_tasks = (
        'loris',
        'id_minter',
        'ingestor',
        'reindexer_worker',
        'reindex_job_creator',
        'complete_reindex',
        'reindex_shard_generator',
        'resharder',
        'transformer',
        'api',
        'monitoring',
        'shared_infra',
        'nginx',
    )
    if (
        task.startswith(sierra_free_tasks) and
        path.startswith('sierra_adapter/')
    ):
        print(
            "~~~ Ignoring %s; sierra_adapter changes don't affect %s tests" %
            (path, task))
        return False

    # Within the sierra_adapter stack, there's an sbt common lib.  If we're
    # in a Sierra project that doesn't use sbt, we can ignore that too.
    sbt_common_free_tasks = (
        's3_demultiplexer',
        'sierra_window_generator',
    )
    if (
        task.startswith(sbt_common_free_tasks) and
        path.startswith('sierra_adapter/common/')
    ):
        print(
            "~~~ Ignoring %s; sierra-common changes don't affect %s tests" %
            (path, task))
        return False

    # The top-level common directory contains some Scala files which are
    # shared across multiple projects.  If we're definitely in a project
    # which doesn't use this sbt-common lib, we can ignore changes to it.
    #
    # The project directory and build.sbt are also Scala-specific.
    sbt_free_tasks = (
        'loris',
        'monitoring',
        'shared_infra',
        's3_demultiplexer',
        'sierra_window_generator',
        'reindex_job_creator',
        'complete_reindex',
        'reindex_shard_generator',
        'resharder',
    )
    if (
        task.startswith(sbt_free_tasks) and
        path.startswith(('common/', 'project/', 'build.sbt'))
    ):
        print(
            "~~~ Ignoring %s; sbt-common changes don't affect %s tests" %
            (path, task))
        return False

    # Otherwise, we were unable to decide if this change was important.
    # We assume that it is, so we'll run tests just in case.
    print("+++ Unable to decide if %s is significant, so assume it is" % path)
    return True


def are_there_job_relevant_changes(changed_files, task):
    """
    Given a list of changed files and a Make task, are there any reasons
    why the output of this task might have changed?

    e.g. if the loris/ directory changes, we should re-run tasks associated
         with Loris.

    Returns a list of reasons why the task should be run.
    """
    reasons = []

    # These tests are usually fast; we always run them rather than trying
    # to keep up-to-date rules of exactly which changed files mean they
    # should run.
    if task in [
        'travis-format'
    ]:
        reasons.append('We always run the %s task' % task)

    # These files are so fundamental to the build process that if they change,
    # we should do a complete rebuild just to be safe.
    if any(f in changed_files for f in [
        'Makefile',
        'functions.Makefile',
    ]):
        reasons.append('Changes to a core Makefile')

    if any(f.startswith('.travis') for f in changed_files):
        reasons.append('Changes to the Travis config')

    if any(f.startswith('builds/') for f in changed_files):
        reasons.append('Changes to the build scripts')
    # Since it's better to run tests when we didn't need to, than skip tests
    # when it was important, we remove any files which we know are safe to
    # ignore, and run tests if there's anything left.
    interesting_changed_files = [
        f for f in changed_files if affects_tests(f, task=task)
    ]

    if interesting_changed_files:
        reasons.append(
            'Changes to the following files mean we need to run tests: %s' %
            ', '.join(interesting_changed_files)
        )

    return reasons


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


def make(task, dry_run=False):
    if dry_run:
        command = ['make', '--dry-run', task]
    else:
        command = ['make', task]
    check_call(command)


def git(*args):
    check_call(['git'] + list(args))


def rreplace(string, old, new, count=None):
    """
    Replace ``old`` with ``new``, starting from the right.
    """
    # Split on the occurrences of ``old``, starting from the right
    parts = string.rsplit(old, count)

    # Then join back together with ``new``
    return new.join(parts)


def unpack_secrets():
    """
    We store our AWS credentials and SSH keys for Travis in an
    encrypted ZIP bundle.

    This unencrypts the credentials, and copies them into place.
    """
    print('*** Loading secrets for Travis', flush=True)

    # Unencrypted the encrypted ZIP file.
    check_call([
        'openssl', 'aes-256-cbc',
        '-K', os.environ['encrypted_83630750896a_key'],
        '-iv', os.environ['encrypted_83630750896a_iv'],
        '-in', 'secrets.zip.enc',
        '-out', 'secrets.zip', '-d'
    ])

    import zipfile
    zf = zipfile.ZipFile('secrets.zip')
    zf.extractall(path='secrets')

    os.makedirs(os.path.join(os.environ['HOME'], '.aws'), exist_ok=True)
    for f in ['config', 'credentials']:
        os.rename(
            src=os.path.join('secrets', f),
            dst=os.path.join(os.environ['HOME'], '.aws', f)
        )

    check_call(['chmod', '400', 'secrets/id_rsa'])
