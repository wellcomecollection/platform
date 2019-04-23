#!/usr/bin/env bash

set -o errexit
set -o nounset

openssl aes-256-cbc -K $encrypted_83630750896a_key -iv $encrypted_83630750896a_iv -in secrets.zip.enc -out secrets.zip -d
unzip secrets.zip
chmod 600 id_rsa

make "$TASK"
