#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

cd /data

env

export JAVA_TOOL_OPTIONS="-Dsbt.ivy.home=/tmp/.ivy2/"

sbt 'project '"$PROJECT"'' ';dockerComposeUp;test;dockerComposeStop'
sbt 'project '"$PROJECT"'' 'scalafmt::test'
