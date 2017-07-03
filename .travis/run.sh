#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o xtrace

# https://graysonkoonce.com/getting-the-current-branch-name-during-a-pull-request-in-travis-ci/
if [[ "$TRAVIS_PULL_REQUEST" == "false" ]]
then
  export BRANCH="$TRAVIS_BRANCH"
else
  export BRANCH="$TRAVIS_PULL_REQUEST_BRANCH"
fi

echo "TRAVIS_BRANCH=$TRAVIS_BRANCH, PR=$TRAVIS_PULL_REQUEST, BRANCH=$BRANCH"

if [[ "$BUILD_TYPE" == "sbt" ]]; then
  ./.travis/run_sbt.sh
fi

# While we're transitioning from build scripts to make tasks, we need to
# check if the TASK variable is defined in Travi, and use make if so.
# Eventually, we'll be able to get rid of this check and call make directly
# from .travis.yml.
if [[ -z "$TASK" ]]; then
  make "$TASK"
fi
