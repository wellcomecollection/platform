#!/usr/bin/env sh

set -o errexit
set -o nounset
set -o verbose

find /data/ -not -path '*WIP*' -name *.js | xargs jshint
find /data/ -not -path '*WIP*' -name *.json | xargs jshint
