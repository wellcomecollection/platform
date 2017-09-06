#!/usr/bin/env bash

set -o errexit
set -o nounset

make "$TASK"

if [[ "$TRAVIS_EVENT_TYPE" == "push" ]]
then
  if [[ "$TASK" != "sbt-test-common" &&
        "$TASK" != "test-lambdas" &&
        "$TASK" != "loris-build" ]]
  then
    TASK="${TASK/build/deploy}"
    TASK="${TASK/test/deploy}"
    TASK="${TASK/test/deploy}"
    make "$TASK"
  elif [[ "$TASK" == "loris-build" ]]
  then
    make loris-publish
  fi
fi
