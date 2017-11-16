#!/usr/bin/env bash
# Install Loris on Ubuntu

set -o errexit
set -o nounset

# Download and install the Loris code itself
echo "*** Downloading the Loris source code"
apt-get install --yes unzip wget
wget "https://github.com/$LORIS_GITHUB_USER/loris/archive/$LORIS_COMMIT.zip"
unzip "$LORIS_COMMIT.zip"
rm "$LORIS_COMMIT.zip"
apt-get remove --yes unzip wget

echo "*** Installing Loris itself"
cd "loris-$LORIS_COMMIT"
python setup.py install
