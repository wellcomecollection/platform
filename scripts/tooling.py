# -*- encoding: utf-8 -*-

import subprocess


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
