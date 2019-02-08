#!/usr/bin/env bash

set -o errexit
set -o nounset

sbt "project $1" compile