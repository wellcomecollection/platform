#!/usr/bin/env bash

set -o errexit
set -o nounset

QUEUE_URL="https://sqs.eu-west-1.amazonaws.com/760097843905/tif_conversion_queue"

while true
do
  MESSAGE="$(aws sqs receive-message --max-number-of-messages=1 --queue-url="$QUEUE_URL")"

  if [[ "$MESSAGE" == "" ]]
  then
    sleep 1
    continue
  fi

  BODY=$(echo "$MESSAGE" | jq -r '.Messages[0].Body')
  KEY=$(echo "$BODY" | jq -r '.key')

  echo "*** Processing $KEY at $(date)"

  pushd $(mktemp -d)
    aws s3 cp "s3://$SOURCE_BUCKET/$KEY" input.jp2
    kdu_expand -precise -fussy -i input.jp2 -o output.tif
    aws s3 cp output.tif "s3://$DST_BUCKET/${KEY/jp2/tif}"
    rm -f input.jp2
    rm -f output.tif
  popd

  RECEIPT=$(echo "$MESSAGE" | jq -r '.Messages[0].ReceiptHandle')
  aws sqs delete-message --queue-url="$QUEUE_URL" --receipt-handle="$RECEIPT"
done
