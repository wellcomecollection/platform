#!/usr/bin/env bash
# This script runs the python-specific build processes.  Specifically, this
# includes:
#
#     * Running a container, which mounts the lambdas dir and runs flake8
#

set -o errexit
set -o nounset
set -o xtrace

docker build ./docker/python3.6_ci -t python3.6_ci
docker run -v $(pwd)/lambdas:/data python3.6_ci:latest
