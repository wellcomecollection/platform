# -*- encoding: utf-8 -*-

import os
import subprocess

from tooling import changed_files, make_decision


def should_run_tests(task, travis_event_type):
    """
    Should we run the tests?

    We skip doing the publish/deploy step when running a build on master that
    doesn't have any relevant changes since the last deploy.
    """
    if travis_event_type == 'push':
        print('*** We always run tests on master!')
        return True

    assert travis_event_type == 'pull_request'

    changed_files = changed_files('HEAD', 'master')

    return make_decision(
        changed_files=changed_files,
        task=task,
        action='run tests'
    )
