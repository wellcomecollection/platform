#!/usr/bin/env bash

set -o errexit
set -o nounset

function build_lambda {
  echo "Attempting to build lambda ..."

  /app/build_lambda.sh

  echo "Done."
}

function install_dependencies {
  echo "Installing dependencies ..."

  if [ -e /data/requirements.txt ]
  then
    echo "Found requirements.txt, installing."
    pip install -r /data/requirements.txt
  else
    echo "No requirements.txt present. Skipping."
  fi
}

function run_tests {
  echo "Testing ..."

  install_dependencies
  FIND_MATCH_PATHS=${FIND_MATCH_PATHS:-/data}
  /app/test.sh "$FIND_MATCH_PATHS"

  echo "Done."
}

function build_lock_file {
  echo "Building lock file ..."

  pip-compile

  echo "Done."
}

if [[ "$OP" == "test" ]]
then
  run_tests
elif [[ "$OP" == "build-lambda" ]]
then
  build_lambda
elif [[ "$OP" == "build-lock-file" ]]
then
  build_lock_file
elif [[ "$OP" == "lint-turtle" ]]
then
  /app/lint_turtle_files.py
else
  echo "Unrecognised operation: $OP! Stopping."

  exit 1
fi
