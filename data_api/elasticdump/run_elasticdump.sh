#!/usr/bin/env sh

set -o errexit
set -o nounset

sqs_response=$(
  aws sqs receive-message --queue-url=$sqs_queue_url --max-number-of-messages=1
)

# The structure of the SQS response is of the form
#
#     {
#       "Messages": [
#         {
#           "Body": "{\"foo\": \"bar\"}",
#           "ReceiptHandle": "AQEB...Xg==",
#           "MD5OfBody": "9423...8fcf",
#           "MessageId": "a82...b14"
#         }
#       ]
#     }
#
receipt_handle=$(echo "$sqs_response" | jq -r '.Messages' | jq -r '.[0]' | jq -r '.ReceiptHandle')

es_url="https://$es_username:$es_password@$es_name.$es_region.aws.found.io:$es_port/$es_index"

elasticdump \
  --input="$es_url" \
  --output=index.txt \
  --type=data

cat index.json | gzip > index.txt.gz

aws s3 cp index.txt.gz s3://$infra_bucket/elasticdump/$(date +"%s")_$es_index.txt.gz

aws sqs delete-message \
  --queue-url=$sqs_queue_url \
  --receipt-handle=$receipt_handle

exit 0
