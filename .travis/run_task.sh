#!/usr/bin/env bash

set -o errexit
set -o nounset

TASK=$1
SHOULD_DEPLOY=$2

make "$TASK"

if [[ "$SHOULD_DEPLOY" == "false" ]]; then
  echo "SHOULD_DEPLOY is false, so exiting before running a deploy"
  exit 0
fi

# Rename the task for the deploy step.
# TODO: Make the naming scheme more consistent!

if [[ "$TASK" == "loris-build" ]]; then
  make loris-publish
elif [[ "$TASK" == "miro_preprocessor-test" ]]; then
  make miro_preprocessor-publish
else
  TASK="${TASK/build/deploy}"
  TASK="${TASK/test/deploy}"
  make "$TASK"
fi