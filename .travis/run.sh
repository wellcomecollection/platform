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
elif [[ "$BUILD_TYPE" == "python" ]]; then
  ./.travis/run_python.sh
fi
