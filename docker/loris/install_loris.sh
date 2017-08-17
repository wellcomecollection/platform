#!/usr/bin/env bash
# Install Loris on Ubuntu

set -o errexit
set -o nounset

LORIS_COMMIT="400bc3979a7dc3ff938abff84665c2731f750a69"

# Install dependencies.  We don't include Apache because we're running
# Loris with UWSGI and nginx, not Apache.
apt-get install -y libjpeg-turbo8-dev libfreetype6-dev zlib1g-dev \
    liblcms2-dev liblcms2-utils libtiff5-dev libwebp-dev

# Download and install the Loris code itself
apt-get install -y unzip wget
wget "https://github.com/loris-imageserver/loris/archive/$LORIS_COMMIT.zip"
unzip "$LORIS_COMMIT.zip"
rm "$LORIS_COMMIT.zip"
apt-get remove -y unzip wget

# Required or setup.py complains
useradd -d /var/www/loris -s /sbin/false loris

cd "loris-$LORIS_COMMIT"
pip install -r requirements.txt
python setup.py install
