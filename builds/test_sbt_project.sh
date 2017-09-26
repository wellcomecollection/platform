#!/usr/bin/env bash

set -o errexit
set -o nounset

ROOT=$(git rev-parse --show-toplevel)

$ROOT/builds/docker_run.py --dind -- \
	--volume ~/.sbt:/root/.sbt \
  --volume ~/.ivy2:/root/.ivy2 \
  sbt_test "project $PROJECT" ";dockerComposeUp;test;dockerComposeStop"
