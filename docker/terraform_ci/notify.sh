#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

TOPIC_NAME=$1
MESSAGE_FILE=$2

TOPIC_ARN=$(aws sns list-topics | jq .Topics[].TopicArn -r | grep "$TOPIC_NAME" | tail -n 1)

if [ -e $MESSAGE_FILE ]; then
    aws sns publish \
        --topic-arn "$TOPIC_ARN" \
        --message "file://$MESSAGE_FILE"
    echo "Notification sent to $TOPIC_ARN"
else
    echo "$MESSAGE_FILE result not found!"
    exit 1
fi