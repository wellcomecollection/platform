#!/usr/bin/env bash

set -o errexit
set -o nounset

# Install dependencies.  We don't include Apache because we're running
# Loris with UWSGI and nginx, not Apache.
echo "*** Installing Ubuntu imaging dependencies"
apt-get install -y libffi-dev libjpeg-turbo8-dev libfreetype6-dev zlib1g-dev \
    liblcms2-dev liblcms2-utils libssl-dev libtiff5-dev libwebp-dev

# Required or setup.py complains
echo "*** Creating Loris user"
useradd -d /var/www/loris -s /sbin/false loris

echo "*** Installing Loris dependencies"
pip install -r /requirements.txt
