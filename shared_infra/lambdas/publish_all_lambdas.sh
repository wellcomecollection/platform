#!/usr/bin/env sh

set -o errexit
set -o nounset

ROOT=$(git rev-parse --show-toplevel)

for f in $(find $ROOT/shared_infra/lambdas -name src -type d)
do
  name=$(basename $(dirname "$f"))
  echo "Building Lambda $name"
  $ROOT/builds/docker_run.py --aws -- \
    --volume $ROOT:/repo \
    publish_lambda_zip \
    "shared_infra/lambdas/$name/src" --key="lambdas/lambdas/$name.zip" --bucket="$INFRA_BUCKET"
done