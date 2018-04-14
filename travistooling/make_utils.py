# -*- encoding: utf-8

from travistooling.shell_utils import check_call


def make(*args):
    """Run a Make command, and check it completes successfully."""
    check_call(['make'] + list(args))
