#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Should we run the deploy step on this Travis PR?

We skip doing the publish/deploy step on jobs that don't have any
relevant changes compared to master.

Exits with code 1 if we should deploy, 0 otherwise.
"""

import os
import subprocess
import sys

from tooling import are_there_job_relevant_changes, changed_files


if __name__ == '__main__':
    subprocess.check_call(['git', 'fetch', 'origin'])

    changed_files = changed_files(os.environ['TRAVIS_COMMIT_RANGE'])
    task = os.environ['TASK']

    if task in [
        'check-format',
        'sbt-test-common',
        'lambdas-test',
    ]:
        print('*** Task %s does not have a deploy step')
        sys.exit(0)

    reasons = are_there_job_relevant_changes(
        changed_files=changed_files, task=task
    )
    if reasons:
        print('*** Reasons to run the deploy step on this change:')
        for r in reasons:
            print('***   - %s' % r)
        sys.exit(1)
    else:
        print('*** No reasons to deploy this change!')
        sys.exit(0)
