#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Should we run tests on this Travis PR?

We skip running tests on PRs that don't have any relevant changes
compared to master.

Exits with code 1 if we should run tests, 0 otherwise.
"""

import os
import subprocess

from tooling import changed_files, make_decision


if __name__ == '__main__':
    subprocess.check_call(['git', 'fetch', 'origin'])

    changed_files = changed_files('HEAD', 'master')
    task = os.environ['TASK']

    make_decision(
        changed_files=changed_files,
        task=task,
        action='run tests'
    )
