#!/usr/bin/env sh

set -o errexit
set -o nounset
set -o xtrace

CONF_FILE=/opt/loris/etc/loris.conf

if [[ -f "$CONF_FILE" ]]
then
  echo "Config is already present; skipping fetch from AWS..."
else
  echo "Fetching config from AWS $INFRA_BUCKET/$CONFIG_KEY ..."

  aws s3 ls s3://$INFRA_BUCKET/$CONFIG_KEY
  aws s3 cp s3://$INFRA_BUCKET/$CONFIG_KEY /opt/loris/etc/loris2.conf
fi

echo "=== config ==="
cat /opt/loris/etc/loris2.conf
echo "=============="

echo "Starting uwsgi..."
/usr/bin/uwsgi --ini /etc/uwsgi/uwsgi.ini
