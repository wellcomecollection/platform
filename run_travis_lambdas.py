#!/usr/bin/env python
# -*- encoding: utf-8
"""
Usage: run_travis_lambdas.py (test|publish)
"""

import os
import subprocess
import sys


if __name__ == '__main__':
    try:
        verb = sys.argv[1]
        assert verb in ('test', 'publish')
    except (AssertionError, IndexError):
        sys.exit(__doc__.strip())

    results = {}

    names = os.environ['TRAVIS_LAMBDAS'].replace('\\\n', ' ').split()

    for lambda_name in names:
        print('===  Starting Lambda task for %s ===' % lambda_name)

        env = os.environ.copy()
        env['TASK'] = '%s-%s' % (lambda_name, verb)

        try:
            subprocess.check_call(['python', 'run_travis_task.py'], env=env)
        except subprocess.CalledProcessError:
            outcome = 'FAILED'
        else:
            outcome = 'OK'

        results[lambda_name] = outcome

        print(
            '=== Completed Lambda task for %s [%s] ===' %
            (lambda_name, outcome)
        )

    print('')
    print('=== SUMMARY ===')
    for (name, outcome) in sorted(results.items()):
        print('%s %s' % (name.ljust(30), outcome))

    if set(results.values()) == 'OK':
        sys.exit(0)
    else:
        sys.exit(1)
