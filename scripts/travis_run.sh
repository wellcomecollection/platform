#!/usr/bin/env bash

set -o errexit
set -o nounset

make "$TASK"

if [[ "$TRAVIS_EVENT_TYPE" == "push" ]]
then
  if [[ "$TASK" != "sbt-test-common" && "$TASK" != "test-lambdas" ]]
  then
    TASK="${TASK/build/deploy}"
    TASK="${TASK/test/deploy}"
    make "$TASK"
  fi
fi
