#!/usr/bin/env bash
# This script runs the sbt-specific build processes.  Specifically, this
# includes:
#
#     * Compiling/testing the code
#     * Building release applications
#     * Pushing Docker images to ECR
#

set -o errexit
set -o nounset

./scripts/run_sbt_tests.sh

./.travis/deploy_sbt.sh