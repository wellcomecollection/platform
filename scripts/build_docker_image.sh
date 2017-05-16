#!/usr/bin/env bash
# Build a Docker image for an application.

set -o errexit
set -o nounset

sbt "project $PROJECT" stage

docker build \
  --build-arg project="$PROJECT" \
  --build-arg config_bucket="$CONFIG_BUCKET" \
  --build-arg build_env="$BUILD_ENV" \
  --tag="$TAG" .

exit 0
