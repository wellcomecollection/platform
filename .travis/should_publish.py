# -*- encoding: utf-8 -*-

import os
import subprocess

from tooling import changed_files, make_decision


def should_publish(task, travis_event_type):
    """
    Should we run the publish step?

    We skip doing the publish/deploy step when running a build on master that
    doesn't have any relevant changes since the last deploy.
    """
    if travis_event_type == 'pull_request':
        print('*** We never publish from pull requests!')
        return False

    assert travis_event_type == 'push'

    if task in [
        'check-format',
        'sbt-test-common',
        'lambdas-test',
    ]:
        print('*** Task %s does not have a publish step')
        return False

    subprocess.check_call(['git', 'fetch', 'origin'])

    return make_decision(
        changed_files=changed_files(os.environ['TRAVIS_COMMIT_RANGE']),
        task=task,
        action='run a publish'
    )
