#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

$GATLING_HOME/bin/gatling.sh -s $SIMULATION
