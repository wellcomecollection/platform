#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

cd /data

if [[ "$OP" == "lint" ]]
then
  echo "Linting Python"
  flake8 --exclude six.py,six-*,structlog*,simplejson* **/*.py --ignore=E501
elif [[ "$OP" == "test" ]]
then
  echo "Testing Lambdas"

  ./install_lambda_deps.sh
  ./test_lambdas.sh

elif [[ "$OP" == "install-deps" ]]
then
  echo "Installing Lambda dependencies"
  ./install_lambda_deps.sh

else
  echo "Unrecognised operation: $OP! Stopping."
  exit 1
fi
