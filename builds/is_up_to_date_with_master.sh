#!/usr/bin/env bash
# This script checks if the current Terraform code is up-to-date with the
# current master.  This avoids annoying problems where Terraform goes
# backwards or breaks already-applied changes.
#
# Consider the following scenario:
#
#     * --- * --- X --- Z                 master
#                  \
#                   Y --- Y --- Y         feature branch
#
# We cut a feature branch at X, then applied commits Y.  Meanwhile master
# added commit Z, and ran `terraform apply`.  If we run `terraform apply` on
# the feature branch, this would revert the changes in Z!  We'd rather the
# branches looked like this:
#
#     * --- * --- X --- Z                 master
#                        \
#                         Y --- Y --- Y   feature branch
#
# So that the commits in the feature branch don't unintentionally revert Z.
#
# This script will check if this is the case, exiting with code 0 if so,
# code 1 if not.

set -o errexit
set -o nounset

# Ensure we have an up-to-date view of the remote master
git fetch origin

if ! git merge-base --is-ancestor origin/master HEAD
then
  echo "Your branch is not up-to-date with current master.  If you make "
  echo "Terraform changes, you may unintentionally revert changes from "
  echo "new commits on master."
  echo

  if [[ $(git rev-parse --abbrev-ref) == "HEAD" ]]
  then
    echo 'Run `git pull origin master` to catch up.'
  else
    echo 'Run `git rebase origin/master` to catch up.'
  fi
  exit 1
else
  echo "Your branch is up-to-date with current master"
fi
