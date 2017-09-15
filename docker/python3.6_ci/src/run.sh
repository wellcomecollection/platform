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

  if [ -e ./install_deps.sh ]
  then
    ./install_deps.sh
    echo "Done."
  else
    echo "No install_deps.sh present."
  fi
}

function lint_python {
  echo "Linting ..."

  flake8 --exclude target  --ignore=E501

  echo "Done."
}

function run_tests {
  echo "Testing ..."

  install_dependencies
  FIND_MATCH_PATHS=${FIND_MATCH_PATHS:-/data}
  /app/test.sh "$FIND_MATCH_PATHS"

  echo "Done."
}

function check_is_master {
  echo "Checking up to date with master ..."

  /app/is_up_to_date_with_master.py

  echo "Done."
}

if [[ "$OP" == "lint" ]]
then
  lint_python
elif [[ "$OP" == "test" ]]
then
  run_tests
elif [[ "$OP" == "install-deps" ]]
then
  install_dependencies
elif [[ "$OP" == "build-lambda" ]]
then
  build_lambda
elif [[ "$OP" == "is-master-head" ]]
then
  check_is_master
else
  echo "Unrecognised operation: $OP! Stopping."

  exit 1
fi
