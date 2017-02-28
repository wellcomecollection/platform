#!/bin/bash

set -x
set -o errexit

for project in api
do
    sbt "project $project" -DconfigBucket=$CONFIG_BUCKET -Denv=$BUILD_ENV ecr:push
done