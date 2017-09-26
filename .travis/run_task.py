#!/usr/bin/env python
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
import subprocess
import sys

from should_publish import should_publish
from should_run_tests import should_run_tests
from tooling import make


def main():
    travis_event_type = os.environ['TRAVIS_EVENT_TYPE']
    task = os.environ['TASK']


    if should_run_tests(task=task, travis_event_type=travis_event_type):
        print("*** We're going to run tests")
    else:
        print("*** We don't need to run tests, exiting early")
        return 0

    make(task)


    if should_publish(task=task, travis_event_type=travis_event_type):
        print("*** We're going to run the publish task")
    else:
        print("*** We don't need the publish task, exiting early")
        return 0

    if task == 'loris-build':
        make('loris-publish')
    elif task == 'miro_preprocessor-test':
        make('miro_preprocessor-publish')
    else:
        task = TASK.replace('build', 'deploy').replace('test', 'deploy')
        make(task)

    return 0


if __name__ == '__main__':
    sys.exit(main())
