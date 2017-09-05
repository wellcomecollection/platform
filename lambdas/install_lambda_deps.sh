#!/usr/bin/env bash
# Install dependencies for our Lambdas.
#
# This looks for a requirements.txt file in individual Lambdas,
# and then installs them into the Lambda directory, as required
# before the ZIP bundle is uploaded to AWS.

set -o errexit
set -o nounset

# Directory containing this script
LAMBDA_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
COMMON_LIB="$LAMBDA_DIR/common"


# Install dependencies for a given Lambda directory
build_lambda() {
  lambda_dir="$1/src"
  target_dir="$1/target"

  rm -rf $target_dir
  cp -r $lambda_dir $target_dir

  echo "*** Installing $COMMON_LIB dependencies for $lambda_dir"

  pip3 install $COMMON_LIB --target "$target_dir" --upgrade >> pip_install.log

  if [[ -f "$lambda_dir/requirements.txt" ]]
  then
    echo "*** Found a requirements.txt"
    pip3 install --requirement "$lambda_dir/requirements.txt" --target "$target_dir" --upgrade >> pip_install.log
  else
    echo "*** No requirements.txt found, skipping"
  fi

  echo ""
}

for dir in $(find "$LAMBDA_DIR" \( ! -regex '.*/\..*' \) -mindepth 1 -maxdepth 1 -type d )
do
  if [ "$COMMON_LIB" != "$dir" ]; then
    build_lambda "$dir"
  fi
done
