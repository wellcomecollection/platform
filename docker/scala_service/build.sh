#!/usr/bin/env bash
# Build a Docker image for a service (intended to be run from the project root)

set -o errexit
set -o nounset
set -o xtrace

if [[ "$PROJECT" == "common" ]]
then
  echo "Common lib doesn't have a Docker container; skipping deploy..."
  exit 0
fi

export VERSION="${VERSION:-0.0.1}"
export BUILD_ENV="${BUILD_ENV:-dev}"
export RELEASE_ID="$VERSION-$(git rev-parse HEAD)_$BUILD_ENV"
export TAG="$AWS_ECR_REPO/uk.ac.wellcome/$PROJECT:$RELEASE_ID"

sbt "project $PROJECT" stage

TARGET="$(pwd)/$PROJECT/target/universal/stage"

cd docker/scala_service

docker build \
    --build-arg project="$PROJECT" \
    --build-arg target="$TARGET" \
    --tag="$TAG" .

export AWS_DEFAULT_REGION=eu-west-1

./scripts/push_docker_image_to_ecr.sh

exit 0