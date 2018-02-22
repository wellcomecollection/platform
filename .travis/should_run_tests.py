# -*- encoding: utf-8 -*-

from __future__ import print_function

import os

from travistooling import (
    changed_files, check_call, fprint as print, make_decision
)


def should_run_tests(task, travis_event_type):
    """
    Should we run the tests?

    We skip doing the publish/deploy step when running a build on master that
    doesn't have any relevant changes since the last deploy.
    """
    if travis_event_type == 'cron':
        print('*** We always run tests in cron!')
        return True

    assert travis_event_type in ('pull_request', 'push')

    if travis_event_type == 'pull_request':
        files = changed_files('HEAD', 'master')
    else:
        check_call(['git', 'fetch', 'origin'])
        files = changed_files(os.environ['TRAVIS_COMMIT_RANGE'])

    return make_decision(
        changed_files=files,
        task=task,
        action='run tests'
    )
