# -*- encoding: utf-8 -*-

from __future__ import print_function

import os
import subprocess

from travistooling import changed_files, fprint as print, make_decision


def should_publish(task, travis_event_type):
    """
    Should we run the publish step?

    We skip doing the publish/deploy step when running a build on master that
    doesn't have any relevant changes since the last deploy.
    """
    if travis_event_type in ('cron', 'pull_request'):
        print('*** We never publish from cron or pull requests!')
        return False

    assert travis_event_type == 'push'

    if task in [
        'check-format',
    ]:
        print('*** Task %s does not have a publish step' % task)
        return False

    subprocess.check_call(['git', 'fetch', 'origin'])

    return make_decision(
        changed_files=changed_files(os.environ['TRAVIS_COMMIT_RANGE']),
        task=task,
        action='run a publish'
    )
