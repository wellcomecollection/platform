# -*- encoding: utf-8

import subprocess
import sys


def make(*args):
    """Run a Make command, and check it completes successfully."""
    cmd = ['make'] + list(args)
    print('Running %r' % ' '.join(cmd))
    try:
        return subprocess.check_call(cmd)
    except subprocess.CalledProcessError as err:
        print(err)
        sys.exit(err.returncode)
