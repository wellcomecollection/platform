#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o xtrace

docker build ./docker/js_ci -t js_ci
docker run -v $(pwd)/$PROJECT:/data js_ci:latest
