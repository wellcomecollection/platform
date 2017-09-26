#!/usr/bin/env bash

set -o errexit
set -o nounset

ROOT=$(git rev-parse --show-toplevel)

$ROOT/builds/docker_run.py --dind --sbt -- \
  sbt_image_builder --project="$PROJECT"
