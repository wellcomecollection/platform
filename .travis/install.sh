#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

BUILD_TYPE=${BUILD_TYPE:-make}

if [[ "$BUILD_TYPE" == "sbt" ]]
then
  ./.travis/install_sbt.sh
fi

# TODO: Because we're midway through the migration to make, the TASK variable
# isn't always defined.  This sets a default value if it doesn't exist.
if [[ "${TASK:-not-docker-build-nginx}" == "docker-build-nginx" ]]
then
  # Install the AWS tools so we can log in to ECR
  pip install --upgrade --user awscli
fi
