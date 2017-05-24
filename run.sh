#!/usr/bin/env sh

set -o errexit
set -o nounset

echo "Running project $PROJECT"

echo "Fetching config from AWS..."
aws s3 ls s3://$CONFIG_BUCKET/config/$BUILD_ENV/$PROJECT.ini
aws s3 cp s3://$CONFIG_BUCKET/config/$BUILD_ENV/$PROJECT.ini /opt/docker/conf/application.ini

echo "=== config ==="
cat /opt/docker/conf/application.ini
echo "=============="

echo "Starting Java binary..."
/opt/docker/bin/$PROJECT
