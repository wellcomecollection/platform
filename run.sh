#!/usr/bin/env sh

set -o errexit
set -o nounset
set -o xtrace

echo "Running project $PROJECT"

echo "Fetching config from AWS..."
aws s3 ls s3://$INFRA_BUCKET/$CONFIG_KEY
aws s3 cp s3://$INFRA_BUCKET/$CONFIG_KEY /opt/docker/conf/application.ini

echo "=== config ==="
cat /opt/docker/conf/application.ini
echo "=============="

echo "Starting Java binary..."
/opt/docker/bin/$PROJECT
