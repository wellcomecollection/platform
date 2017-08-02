#!/usr/bin/env bash
# Install Loris on Ubuntu

set -o errexit
set -o nounset

# Install dependencies.  We don't include Apache because we're running
# Loris with UWSGI and nginx, not Apache.
apt-get install -y libjpeg-turbo8-dev libfreetype6-dev zlib1g-dev \
    liblcms2-dev liblcms2-utils libtiff5-dev libwebp-dev

# Download and install the Loris code itself
apt-get install -y unzip wget
wget https://github.com/loris-imageserver/loris/archive/v2.1.0-final.zip
unzip v2.1.0-final.zip
rm v2.1.0-final.zip
apt-get remove -y unzip wget

# Required or setup.py complains
useradd -d /var/www/loris -s /sbin/false loris

cd loris-2.1.0-final
pip install -r requirements.txt
python setup.py install --image-cache=/mnt/efs/image_cache --info-cache=/mnt/efs/info_cache