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
