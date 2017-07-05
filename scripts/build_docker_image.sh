#!/usr/bin/env bash
# Build a Docker image for an application.

set -o errexit
set -o nounset
set -o xtrace

sbt "project $PROJECT" stage

docker build --build-arg project="$PROJECT" --tag="$TAG" .

mkdir -p .releases
echo "$RELEASE_ID" > ".releases/$PROJECT"

exit 0