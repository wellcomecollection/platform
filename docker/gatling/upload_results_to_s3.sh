#!/usr/bin/env bash

set -o errexit
set -o nounset

LAST_RESULT="$(find /opt/gatling/results -maxdepth 1 -type d | sort | tail -n 1)"

aws s3 cp --acl=public-read --recursive "$LAST_RESULT" "s3://wellcome-platform-dash/gatling/$(basename $LAST_RESULT)"
