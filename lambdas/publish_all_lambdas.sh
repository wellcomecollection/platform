#!/usr/bin/env sh

set -o errexit
set -o nounset

ROOT=$(git rev-parse --show-toplevel)

for f in $(find $LAMBDAS -name src -type d)
do
  name=$(basename $(dirname "$f"))
  echo "Building Lambda $name"
  $ROOT/builds/docker_run.py --aws -- \
    --volume $ROOT:/repo \
    publish_lambda_zip \
    "lambdas/$name/src" --key="lambdas/$name" --bucket="$INFRA_BUCKET"
done