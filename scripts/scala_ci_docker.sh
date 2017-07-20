#!/usr/bin/env bash

set -o errexit
set -o nounset

PROJECT=$1
OP=$2
INFRA_BUCKET=$3

echo "Running Scala CI $OP for $PROJECT."

docker run \
    --net host \
    -v ~/.ivy2:/tmp/.ivy2 \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v $$(pwd):/data \
    -e PROJECT=$PROJECT \
    -e INFRA_BUCKET=$INFRA_BUCKET \
    -e OP=$OP \
    -e ES_JAVA_OPTS="-Xms750m -Xmx750m" \
    scala_ci:latest

echo "Done."