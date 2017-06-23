#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

if [[ "$BUILD_TYPE" == "sbt" ]]
then
  ./.travis/install_sbt.sh
fi
