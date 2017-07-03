#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose


jshint /data/**/*.js*
