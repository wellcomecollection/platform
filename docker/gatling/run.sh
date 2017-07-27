#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

$GATLING_HOME/bin/gatling.sh -s $SIMULATION
/opt/gatling/upload_results_to_s3.sh
