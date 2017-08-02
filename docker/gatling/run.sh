#!/usr/bin/env bash

set -o nounset

# Parse test parameters, and then re-export them so the same variables
# are available in the Scala test.
export USE_CLOUDFRONT=${USE_CLOUDFRONT:-false}
export IMAGES_PER_ARTICLE=${IMAGES_PER_ARTICLE:-15}
export IMAGES_PER_SEARCH=${IMAGES_PER_SEARCH:-20}
export USERS_TO_SIMULATE=${USERS_TO_SIMULATE:-5}

SUMMARY=${SUMMARY:-Gatling run}

export DESCRIPTION="$SUMMARY (CF=$USE_CLOUDFRONT, article=$IMAGES_PER_ARTICLE, search=$IMAGES_PER_SEARCH, users=$USERS_TO_SIMULATE)"

$GATLING_HOME/bin/gatling.sh --simulation $SIMULATION --run-description="$DESCRIPTION"
GATLING_STATUS=$?

LAST_RESULT="$(find /opt/gatling/results -maxdepth 1 -type d | sort | tail -n 1)"
S3_LOCATION="s3://$S3_BUCKET/gatling/$(basename $LAST_RESULT)"

aws s3 cp \
    --acl=public-read \
    --recursive "$LAST_RESULT" \
    $S3_LOCATION

# Always push results to load_test_results topic
/opt/gatling/notify.sh $RESULTS_TOPIC_ARN "$LAST_RESULT/js/assertions.json"

# On failure push results to load_test_failure_alarm topic so we can alarm
if (( GATLING_STATUS != 0 )); then
    echo "Load test failed, pushing to SNS."

    /opt/gatling/notify.sh "$FAILED_TOPIC_ARN" "$LAST_RESULT/js/assertions.json"
fi
