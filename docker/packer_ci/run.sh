#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

cd /data

packer build amis.json

