#!/usr/bin/env bash
# This script runs the python-specific build processes.  Specifically, this
# includes:
#
#     * Running a container, which mounts the lambdas dir and runs flake8
#

set -o errexit
set -o nounset
set -o xtrace

docker build ./docker/python3.6 -t python3.6
docker run -v ./lambdas:/data python3.6:latest
