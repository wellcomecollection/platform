#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

BUILD_TYPE=${BUILD_TYPE:-make}

if [[ "$BUILD_TYPE" == "sbt" ]]
then
  ./.travis/install_sbt.sh
fi

if [[ "${TASK:-foo}" == "docker-build-nginx" ]]
then
  # Install the AWS tools so we can log in to ECR
  pip install --upgrade --user awscli
fi
