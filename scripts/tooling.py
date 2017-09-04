# -*- encoding: utf-8 -*-

import errno
import os
import shlex
import subprocess


# Root of the Git repository
ROOT = subprocess.check_output([
    'git', 'rev-parse', '--show-toplevel']).decode('ascii').strip()

# Hash of the current commit
CURRENT_COMMIT = subprocess.check_output([
    'git', 'rev-parse', 'HEAD']).decode('ascii').strip()

# Environment from environment environment variable!
DEFAULT_BUILD_ENV = 'dev'
PLATFORM_ENV = os.getenv('PLATFORM_ENV', DEFAULT_BUILD_ENV)


def changed_files(commit_range):
    """
    Returns a set of changed files in a given commit range.

    :param commit_range: A list of arguments to pass to ``git diff``.

    """
    files = set()
    command = ['git', 'diff', '--name-only'] + commit_range
    diff_output = subprocess.check_output(command).decode('ascii')
    for line in diff_output.splitlines():
        filepath = line.strip()
        if filepath:
            files.add(filepath)
    return files


def write_release_id(project, release_id):
    """
    Write a release ID to the .releases directory in the root of the repo.
    """
    releases_dir = os.path.join(ROOT, '.releases')
    os.makedirs(releases_dir, exist_ok=True)

    release_file = os.path.join(releases_dir, project)
    with open(release_file, 'w') as f:
        f.write(release_id)


def mkdir_p(path):
    """Create a directory if it doesn't already exist."""
    # https://stackoverflow.com/a/600612/1558022
    try:
        os.makedirs(path)
    except OSError as exc:  # Python >2.5
        if exc.errno == errno.EEXIST and os.path.isdir(path):
            pass
        else:
            raise
