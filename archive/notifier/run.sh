#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o xtrace

echo "Substitute environment variables into /opt/docker/conf/application.conf.template"
envsubst < /opt/docker/conf/application.conf.template > /opt/docker/conf/application.conf

echo "Config: /opt/docker/conf/application.conf ==="
cat /opt/docker/conf/application.conf

/opt/docker/bin/notifier
