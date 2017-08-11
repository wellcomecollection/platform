#!/usr/bin/env sh

set -o errexit
set -o nounset
set -o verbose

find /data/*.js | xargs jshint
find /data/*.json | xargs jshint
