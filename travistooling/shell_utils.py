# -*- encoding: utf-8

import subprocess
import sys


def check_call(cmd):
    """
    A wrapped version of subprocess.check_call() that doesn't print a
    traceback if the command errors.
    """
    print('*** Running %r' % ' '.join(cmd))
    try:
        return subprocess.check_call(cmd)
    except subprocess.CalledProcessError as err:
        print(err)
        sys.exit(err.returncode)
