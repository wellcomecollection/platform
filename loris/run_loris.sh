#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o xtrace

echo "Substitute environment variables into $LORIS_CONF_FILE"
envsubst < /opt/loris/etc/loris2.conf/template > $LORIS_CONF_FILE

echo "Config: $LORIS_CONF_FILE ==="
cat $LORIS_CONF_FILE

/usr/bin/uwsgi --ini /etc/uwsgi/uwsgi.ini
