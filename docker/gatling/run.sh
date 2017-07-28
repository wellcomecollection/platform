#!/usr/bin/env bash

set -o nounset


$GATLING_HOME/bin/gatling.sh -s $SIMULATION
GATLING_STATUS=$?

LAST_RESULT="$(find /opt/gatling/results -maxdepth 1 -type d | sort | tail -n 1)"
S3_LOCATION="s3://wellcome-platform-dash/gatling/$(basename $LAST_RESULT)"

aws s3 cp \
    --acl=public-read \
    --recursive "$LAST_RESULT" \
    $S3_LOCATION

if [ $GATLING_STATUS -ne 0 ]; then
    echo "Load test failed, pushing to SNS."

    /opt/gatling/notify.sh \
        load_test_failure_alarm \
        "$S3_LOCATION/js/assertions.json"
fi
