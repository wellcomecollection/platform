#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

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

# If we're on master, build a Docker image and push it to ECR
export AWS_DEFAULT_REGION=eu-west-1

if [[ "$BRANCH" != "master" ]]
then
  echo "Not on master; skipping deploy..."
  exit 0
fi

if [[ "$BRANCH" == "master" && "$PROJECT" == "common" ]]
then
  echo "Not an application (common); skipping deploy"
  exit 0
fi

sbt "project $PROJECT" stage
export RELEASE_ID="0.0.1-$(git rev-parse HEAD)_prod"
$(aws ecr get-login)

docker build  \
        --build-arg project=$PROJECT \
        --build-arg config_bucket=$CONFIG_BUCKET \
        --build-arg build_env=prod \
        --tag=$AWS_ECR_REPO/uk.ac.wellcome/$PROJECT:$RELEASE_ID .

docker push $AWS_ECR_REPO/uk.ac.wellcome/$PROJECT:$RELEASE_ID
echo "New container image is $RELEASE_ID"

export TARGET_FILE="terraform_$PROJECT.tfvars"
echo "release_id_$PROJECT = \"$RELEASE_ID\"" > "$TARGET_FILE"
aws s3 cp "$TARGET_FILE" "s3://$CONFIG_BUCKET/$TARGET_FILE"

exit 0
