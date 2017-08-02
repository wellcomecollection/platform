#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

TOPIC_ARN=$1
MESSAGE_FILE=$2

if [ -e $MESSAGE_FILE ]; then
    aws sns publish \
        --topic-arn "$TOPIC_ARN" \
        --message "file://$MESSAGE_FILE"
    echo "Notification sent to $TOPIC_ARN"
else
    echo "$MESSAGE_FILE result not found!"
    exit 1
fi