#!/usr/bin/env bash
# Install Loris on Ubuntu

set -o errexit
set -o nounset

# Install dependencies.  We don't include Apache because we're running
# Loris with UWSGI and nginx, not Apache.
apt-get install -y libffi-dev libjpeg-turbo8-dev libfreetype6-dev zlib1g-dev \
    liblcms2-dev liblcms2-utils libssl-dev libtiff5-dev libwebp-dev

# Download and install the Loris code itself
apt-get install -y unzip wget
wget "https://github.com/$LORIS_GITHUB_USER/loris/archive/$LORIS_COMMIT.zip"
unzip "$LORIS_COMMIT.zip"
rm "$LORIS_COMMIT.zip"
apt-get remove -y unzip wget

# Required or setup.py complains
useradd -d /var/www/loris -s /sbin/false loris

# Upgrading pip to ensure we get a recent version as Ubuntu gives us a very old one
# Otherwise we run into issues where deps rely on more recent versions e.g.
# https://github.com/pyca/cryptography/issues/3959
pip install --upgrade pip

cd "loris-$LORIS_COMMIT"
pip install -r /requirements.txt
python setup.py install
