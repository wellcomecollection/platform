#!/usr/bin/env bash

set -o errexit
set -o nounset

if [[ "$OP" == "lint" ]]
then
  echo "Linting ..."

  flake8 --exclude six.py,six-*,structlog*,simplejson* **/*.py --ignore=E501

  echo "Done."
elif [[ "$OP" == "test" ]]
then
  echo "Testing ..."

  ./install_deps.sh
  ./test_lambdas.sh

  echo "Done."
elif [[ "$OP" == "install-deps" ]]
then
  echo "Installing dependencies ..."

  ./install_deps.sh

  echo "Done."
elif [[ "$OP" == "is-master-head" ]]
then
  echo "Checking up to date with master ..."

  /app/is_up_to_date_with_master.py

  echo "Done."
else
  echo "Unrecognised operation: $OP! Stopping."

  exit 1
fi
