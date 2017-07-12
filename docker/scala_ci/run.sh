#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

# Set ivy cache location for sbt
export JAVA_TOOL_OPTIONS="-Dsbt.ivy.home=/tmp/.ivy2/"

function test_project()
{
    cd /data

    sbt 'project '"$PROJECT"'' ';dockerComposeUp;test;dockerComposeStop'

    cd -
}

function lint_project()
{
    cd /data

    sbt 'project '"$PROJECT"'' 'scalafmt::test'

    cd -
}

function build_project()
{
    /scripts/build_sbt_image.py --project="$PROJECT"
    /scripts/deploy_docker_to_aws.py --project="$PROJECT" --infra-bucket="$INFRA_BUCKET"
}

if [[ "$OP" == "test" ]]
then
  lint_project
  test_project
elif [[ "$OP" == "build" ]]
then
  lint_project
  test_project
  build_project
else
  echo "Unrecognised operation: $OP! Stopping."
  exit 1
fi
