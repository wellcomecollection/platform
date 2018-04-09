# -*- encoding: utf-8
"""
Git-related utilities used in our Travis scripts.
"""


import subprocess


def git(*args):
    """Run a Git command and return its output."""
    cmd = ['git'] + list(args)
    return subprocess.check_output(cmd).decode('ascii').strip()


# Root of the Git repository
ROOT = git('rev-parse', '--show-toplevel')


def changed_files(*args):
    """
    Returns a set of changed files in a given commit range.

    :param commit_range: Arguments to pass to ``git diff``.
    """
    files = set()
    diff_output = git('diff', '--name-only', *args)

    return set([
        line.strip()
        for line in diff_output.splitlines()
    ])
