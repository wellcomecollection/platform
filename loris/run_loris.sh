#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o xtrace

if [[ -f "$LORIS_CONF_FILE" ]]
then
  echo "Config is already present; skipping fetch from AWS..."
else
  echo "Fetching config from AWS $INFRA_BUCKET/$CONFIG_KEY ..."

  aws s3 ls s3://$INFRA_BUCKET/$CONFIG_KEY
  aws s3 cp s3://$INFRA_BUCKET/$CONFIG_KEY "$LORIS_CONF_FILE"
fi

echo "=== config ==="
cat "$LORIS_CONF_FILE"
echo "=============="

echo "Starting uwsgi..."
/usr/bin/uwsgi --ini /etc/uwsgi/uwsgi.ini
