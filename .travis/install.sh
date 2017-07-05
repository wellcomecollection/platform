#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

BUILD_TYPE=${BUILD_TYPE:-make}

if [[ "$BUILD_TYPE" == "sbt" ]]
then
  ./.travis/install_sbt.sh
fi
