#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

cd /data

export JAVA_TOOL_OPTIONS="-Dsbt.ivy.home=/tmp/.ivy2/"

sbt 'project '"$PROJECT"'' ';dockerComposeUp;test;dockerComposeStop'
sbt 'project '"$PROJECT"'' 'scalafmt::test'

/scripts/build_sbt_image.py --project="$PROJECT"
/scripts/deploy_docker_to_aws.py --project="$PROJECT" --infra-bucket="$INFRA_BUCKET"
