#!/usr/bin/env bash

set -o errexit
set -o nounset

ROOT=$(git rev-parse --show-toplevel)

docker run --tty \
  --volume $ROOT:/repo \
  --volume /var/run/docker.sock:/var/run/docker.sock \
    "$@"
