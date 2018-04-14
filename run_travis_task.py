#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
Because we have a lot of Travis jobs, we try to avoid running unnecessary
tests and deploys.  This speeds up our builds, minimises deployment churn,
and reduces contention on our Travis infrastructure.

How we decide what to build:

                  | Relevant changes | No relevant changes
   ---------------+------------------+---------------------
    pull request  | Run tests        | Don't run tests
                  | Don't deploy     | Don't deploy
   ---------------+------------------+---------------------
    master        | Run tests        | Run tests
                  | Deploy to AWS    | Don't deploy

We always run tests on master so we get consistent build results, that's
less important on master where results are transient.

"""

import os
import sys

from should_publish import should_publish
from travistooling.make_utils import (
    build_report_output,
    get_changed_paths,
    git,
    make,
    should_run_job,
    unpack_secrets
)


def _should_run_tests(task_name, travis_event_type):
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
        changed_paths = get_changed_paths('HEAD', 'master')
    else:
        git('fetch', 'origin')
        changed_paths = get_changed_paths(os.environ['TRAVIS_COMMIT_RANGE'])

    report = should_run_build_task(changed_paths=changed_paths, task=task)
    print(build_report_output(report))

    return bool(report[True])


def main():
    travis_event_type = os.environ['TRAVIS_EVENT_TYPE']
    task = os.environ['TASK']

    if _should_run_tests(task=task, travis_event_type=travis_event_type):
        print("*** We're going to run tests")
    else:
        print("*** We don't need to run tests, exiting early")
        return 0

    unpack_secrets()

    make(task)

    publish_task = task.replace('-build', '-publish')
    publish_task = task.replace('-test', '-publish')

    if should_publish(task=task, travis_event_type=travis_event_type):
        print("*** We're going to run the publish task")
        make(publish_task)
    else:
        print("*** We don't need to actually run the publish task")
        make(publish_task, '--dry-run')

    return 0


if __name__ == '__main__':
    sys.exit(main())
