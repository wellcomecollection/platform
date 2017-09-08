# -*- encoding: utf-8 -*-

import errno
import os
import subprocess


# Root of the Git repository
ROOT = subprocess.check_output([
    'git', 'rev-parse', '--show-toplevel']).decode('ascii').strip()


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
