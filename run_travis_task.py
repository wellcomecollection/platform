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
   ---------------+------------------+
    master        | Run tests        |
                  | Deploy to AWS    |
    --------------+------------------+---------------------
    cron          | Always run tests
                  | Never deploy to AWS

We run a full test suite in the cron tests so that even if we think a file
that has no relevant changes actually affects the tests, we still get
notice fairly quickly of the problem in master.

"""

import os
import sys

from travistooling import (
    build_report_output,
    get_changed_paths,
    git,
    make,
    should_run_build_task,
    unpack_secrets
)


def _should_run_tests(task, travis_event_type):
    """
    Should we run the tests?
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

    result, report = should_run_build_task(
        changed_paths=changed_paths,
        task=task
    )
    print('\n' + build_report_output(report) + '\n')

    return result


def _should_run_publish(task, travis_event_type):
    """
    Should we run the publish step?
    """
    if travis_event_type in ('cron', 'pull_request'):
        print('*** We never publish from cron or pull requests!')
        return False

    assert travis_event_type == 'push'

    git('fetch', 'origin')

    changed_paths = get_changed_paths(os.environ['TRAVIS_COMMIT_RANGE'])
    result, report = should_run_build_task(
        changed_paths=changed_paths,
        task=task
    )
    print('\n' + build_report_output(report) + '\n')

    return result


def main():
    # https://docs.travis-ci.com/user/environment-variables/
    travis_event_type = os.environ['TRAVIS_EVENT_TYPE']
    task = os.environ['TASK']

    if _should_run_tests(task=task, travis_event_type=travis_event_type):
        print("*** We're going to run tests")
    else:
        print("*** We don't need to run tests, exiting early")
        return 0

    unpack_secrets()

    make(task)

    if task in [
        'travis-format',
        'travistooling-test',
    ]:
        print('*** Task %s does not have a publish step' % task)
        return 0

    publish_task = task.replace('-build', '-publish')
    publish_task = task.replace('-test', '-publish')

    if _should_run_publish(task=task, travis_event_type=travis_event_type):
        print("*** We're going to run the publish task")
        make(publish_task)
    else:
        print("*** We don't need to run the publish task")

        # Doing a --dry-run checks that the associated publish task exists,
        # which protects us from merging a branch with no publish task.
        make(publish_task, '--dry-run')

    return 0


if __name__ == '__main__':
    sys.exit(main())
