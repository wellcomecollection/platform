#!/usr/bin/env bash

set -o errexit
set -o nounset

ROOT=$(git rev-parse --show-toplevel)

$ROOT/builds/docker_run.py --dind --sbt --root -- \
  --net host \
  sbt_test "project $PROJECT" ";dockerComposeUp;test;dockerComposeStop"
