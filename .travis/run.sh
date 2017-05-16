#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o xtrace

# https://graysonkoonce.com/getting-the-current-branch-name-during-a-pull-request-in-travis-ci/
if [[ "$TRAVIS_PULL_REQUEST" == "false" ]]
then
  export BRANCH="$TRAVIS_BRANCH"
else
  export BRANCH="$TRAVIS_PULL_REQUEST_BRANCH"
fi

echo "TRAVIS_BRANCH=$TRAVIS_BRANCH, PR=$TRAVIS_PULL_REQUEST, BRANCH=$BRANCH"

# Run the commands for the test.  Chaining them together means we don't
# have to pay the cost of starting the JVM three times.
# http://www.scala-sbt.org/0.12.2/docs/Howto/runningcommands.html
sbt "project $PROJECT" ";dockerComposeUp;test;dockerComposeStop"

# If we're on the master branch and in an application project, we should
# build a new Docker image and push it to ECR.

if [[ "$BRANCH" != "master" ]]
then
  echo "Not on master (BRANCH=$BRANCH); skipping deploy..."
  exit 0
fi

if [[ "$PROJECT" == "common" ]]
then
  echo "Common lib doesn't have a Docker container; skipping deploy..."
  exit 0
fi

export VERSION="0.0.1"
export BUILD_ENV="prod"
export RELEASE_ID="$VERSION-$(git rev-parse HEAD)_$BUILD_ENV"
export TAG="$AWS_ECR_REPO/uk.ac.wellcome/$PROJECT:$RELEASE_ID"

./scripts/build_docker_image.sh

export AWS_DEFAULT_REGION=eu-west-1

./scripts/push_docker_image_to_ecr.sh
