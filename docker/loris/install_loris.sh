#!/usr/bin/env sh
# This script installs Loris in Alpine Linux.

set -o errexit
set -o nounset

DOWNLOAD_URL="https://github.com/loris-imageserver/loris/archive/v2.1.0-final.zip"
DOWNLOAD_PATH="v2.1.0-final.zip"
DOWNLOAD_DIR="loris-2.1.0-final"

apk update

# Start by downloading Loris.  It isn't installable through pip (yet), so
# we have to download a ZIP bundle from GitHub and unpack it from there.
apk add ca-certificates unzip wget
wget --output-document="$DOWNLOAD_PATH" "$DOWNLOAD_URL"
unzip "$DOWNLOAD_PATH"
rm "$DOWNLOAD_PATH"

cd "$DOWNLOAD_DIR"

# We need to create a loris user, or setup.py fails.  This user doesn't
# need a login shell (-s /sbin/false) or a password (-D).
mkdir -p /var/www/loris
adduser -h /var/www/loris -s /sbin/false -D loris

# Loris relies on Pillow, which depends on a number of image libraries.
# Ensure those are installed as well.
apk add build-base jpeg-dev python-dev zlib-dev

# We install Loris by calling setup.py directly, not through pip.
apk add python py-pip py-setuptools
pip install configobj
python setup.py install

# Remove packages that were only needed for installation, and clean up
# the apk cache
apk del build-base ca-certificates py-pip py-setuptools unzip wget
rm -rf /var/cache/apk/*
