#!/usr/bin/env bash
# Install dependencies for our Lambdas.
#
# This looks for a requirements.txt file in the common lib, and on
# individual Lambdas, and then installs them into the Lambda directory,
# as required before the ZIP bundle is uploaded to AWS.
#
# This does not yet populate a .gitignore with the packages it installs.

set -o errexit
set -o nounset

# Directory containing this script
LAMBDA_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

COMMON_LIB="$LAMBDA_DIR/common"


# Install dependencies for a given Lambda directory
build_lambda() {
  lambda_dir="$1"
  echo "*** Installing dependencies for $lambda_dir"

  if [[ -f "$lambda_dir/requirements.txt" ]]
  then
    echo "*** Found a requirements.txt"
    pip3 install --requirement "$lambda_dir/requirements.txt" --target "$lambda_dir" >> pip_install.log
  else
    echo "*** No requirements.txt found, skipping"
  fi

  if [[ -f "$COMMON_LIB/requirements.txt" ]]
  then
    echo "*** Found a requirements.txt in common"
    pip3 install --requirement "$COMMON_LIB/requirements.txt" --target "$lambda_dir" >> pip_install.log
  else
    echo "*** No common requirements.txt, skipping"
  fi

  echo ""
}

for dir in $(find "$LAMBDA_DIR" -mindepth 1 -maxdepth 1 -type d)
do
  build_lambda "$dir"
done
