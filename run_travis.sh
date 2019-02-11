#!/usr/bin/env bash

set -o errexit
set -o nounset

make "$TASK"
if [[ "$TRAVIS_EVENT_TYPE" == "push" && "$TRAVIS_BUILD_STAGE_NAME" == "Services" ]]
then
  make "${TASK/test/publish}"
fi
