#!/usr/bin/env bash
# Install Loris on Ubuntu

set -o errexit
set -o nounset

# Install dependencies.  We don't include Apache because we're running
# Loris with UWSGI and nginx, not Apache.
echo "*** Installing Ubuntu imaging dependencies"
apt-get update
apt-get install --yes libffi-dev libjpeg-turbo8-dev libfreetype6-dev zlib1g-dev \
    liblcms2-dev liblcms2-utils libssl-dev libtiff5-dev libwebp-dev

# Download and install the Loris code itself
echo "*** Downloading the Loris source code"
apt-get install --yes unzip wget
wget "https://github.com/$LORIS_GITHUB_USER/loris/archive/$LORIS_COMMIT.zip"
unzip "$LORIS_COMMIT.zip"
rm "$LORIS_COMMIT.zip"
apt-get remove --yes unzip wget

# Required or setup.py complains
echo "*** Creating Loris user"
useradd -d /var/www/loris -s /sbin/false loris

# Install pip.  We don't use pip from the Ubuntu package repositories
# because it tends to be out-of-date and using it gets issues like:
# https://github.com/pyca/cryptography/issues/3959
apt-get install --yes wget
wget https://bootstrap.pypa.io/get-pip.py
python ./get-pip.py

rm -rf get-pip.py
apt-get remove --yes wget
apt-get autoremove --yes

echo "*** Installing Loris dependencies"
pip install -r /requirements.txt

echo "*** Installing Loris itself"
cd "loris-$LORIS_COMMIT"
python setup.py install

apt-get clean
