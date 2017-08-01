#!/usr/bin/env sh

set -o errexit
set -o nounset
set -o xtrace

echo "Fetching config from AWS..."
aws s3 ls s3://$INFRA_BUCKET/$CONFIG_KEY
aws s3 cp s3://$INFRA_BUCKET/$CONFIG_KEY /opt/loris/etc/loris2.conf

echo "=== config ==="
cat /opt/loris/etc/loris2.conf
echo "=============="

echo "Starting uwsgi..."
/usr/bin/uwsgi --ini /etc/uwsgi/uwsgi.ini
