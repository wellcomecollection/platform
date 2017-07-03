#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o xtrace

docker build ./docker/jslint_ci -t jslint_ci
docker run -v $(pwd)/$PROJECT:/data jslint_ci:latest
