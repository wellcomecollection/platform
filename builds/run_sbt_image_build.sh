#!/usr/bin/env bash

set -o errexit
set -o nounset

ROOT=$(git rev-parse --show-toplevel)

$/scripts/run_docker_in_docker.sh \
	--volume ~/.sbt:/root/.sbt \
  --volume ~/.ivy2:/root/.ivy2 \
  sbt_image_builder --project="$PROJECT"
