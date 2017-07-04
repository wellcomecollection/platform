#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
This script checks if the current Terraform code is up-to-date with the
current master.  This avoids annoying problems where Terraform goes
backwards or breaks already-applied changes.

Consider the following scenario:

    * --- * --- X --- Z                 master
                 \
                  Y --- Y --- Y         feature branch

We cut a feature branch at X, then applied commits Y.  Meanwhile master
added commit Z, and ran `terraform apply`.  If we run `terraform apply` on
the feature branch, this would revert the changes in Z!  We'd rather the
branches looked like this:

    * --- * --- X --- Z                 master
                       \
                        Y --- Y --- Y   feature branch

So that the commits in the feature branch don't unintentionally revert Z.

This script will check if this is the case, exiting with code 0 if so,
code 1 if not.

"""

import subprocess
import sys


# Make sure we have an up-to-date view of the remote master
subprocess.check_call(['git', 'fetch', 'origin'])

# Next ask: what's the common ancestor of our current state and master?
common_ancestor = subprocess.check_output(['git', 'merge-base', 'origin/master', 'HEAD'])

# And is this the tip of master?
master_tip = subprocess.check_output(['git', 'rev-parse', 'origin/master'])

if common_ancestor != master_tip:
    print('Your branch is not up-to-date with current master.  If you run')
    print('terraform apply, you may unintentionally revert changes from')
    print('new commits on master.')
    print()

    branch = subprocess.check_output(['git', 'rev-parse', '--abbrev-ref', 'HEAD'])
    if branch.strip().decode('ascii') == 'master':
        print('Run `git pull origin master` to catch up.')
    else:
        print('Run `git rebase origin/master` to catch up.')
    sys.exit(1)
else:
    print('Your branch is up-to-date with current master!')
    sys.exit(0)
