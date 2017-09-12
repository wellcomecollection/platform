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
import sys

from tooling import are_there_job_relevant_changes, changed_files


if __name__ == '__main__':
    subprocess.check_call(['git', 'fetch', 'origin'])

    changed_files = changed_files('HEAD', 'master')
    task = os.environ['TASK']

    reasons = are_there_job_relevant_changes(
        changed_files=changed_files, task=task
    )
    if reasons:
        print('*** Reasons to run Travis tests on this PR:')
        for r in reasons:
            print('***   - %s' % r)
        sys.exit(1)
    else:
        print('*** No reasons to run Travis tests on this PR!')
        sys.exit(0)
