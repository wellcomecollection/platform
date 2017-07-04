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

# If we're on the master branch and in an application project, we should
# build a new Docker image and push it to ECR.

if [[ "$BRANCH" != "master" ]]
then
  echo "Not on master (BRANCH=$BRANCH); skipping deploy..."
  exit 0
fi

export BUILD_ENV=prod

./.travis/deploy_sbt.sh