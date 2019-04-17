#!/usr/bin/env bash

set -o errexit
set -o nounset

openssl aes-256-cbc -K $encrypted_83630750896a_key -iv $encrypted_83630750896a_iv -in secrets.zip.enc -out secrets.zip -d
unzip secrets.zip

mkdir -p ~/.aws
mv config ~/.aws/config
mv credentials ~/.aws/credentials

mkdir -p ~/.ssh
mv id_rsa ~/.ssh/id_rsa
chmod 600 ~/.ssh/id_rsa

make "$TASK"