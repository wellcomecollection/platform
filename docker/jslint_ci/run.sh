#!/usr/bin/env sh

set -o errexit
set -o nounset
set -o verbose

find /data/ -name *.js | xargs jshint
find /data/ -name *.json | xargs jshint
