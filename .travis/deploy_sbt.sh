#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

if [[ "$PROJECT" == "common" ]]
then
  echo "Common lib doesn't have a Docker container; skipping deploy..."
  exit 0
fi

export VERSION="${VERSION:-0.0.1}"
export BUILD_ENV="${BUILD_ENV:-dev}"
export RELEASE_ID="$VERSION-$(git rev-parse HEAD)_$BUILD_ENV"
export TAG="$AWS_ECR_REPO/uk.ac.wellcome/$PROJECT:$RELEASE_ID"

./scripts/build_docker_image.sh

export AWS_DEFAULT_REGION=eu-west-1

./scripts/push_docker_image_to_ecr.sh
