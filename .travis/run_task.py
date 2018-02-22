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

from __future__ import print_function

import os
import sys

from should_publish import should_publish
from should_run_tests import should_run_tests
from travistooling import make, rreplace


def main():
    travis_event_type = os.environ['TRAVIS_EVENT_TYPE']
    task = os.environ['TASK']

    if should_run_tests(task=task, travis_event_type=travis_event_type):
        print("*** We're going to run tests", flush=True)
    else:
        print("*** We don't need to run tests, exiting early", flush=True)
        return 0

    make(task)

    publish_task = rreplace(task, 'build', 'publish', count=1)
    publish_task = rreplace(task, 'test', 'publish', count=1)

    if should_publish(task=task, travis_event_type=travis_event_type):
        print("*** We're going to run the publish task", flush=True)
        dry_run = False
    else:
        print("*** We don't need to actually run the publish task", flush=True)
        dry_run = True

    make(publish_task, dry_run=dry_run)

    return 0


if __name__ == '__main__':
    sys.exit(main())
