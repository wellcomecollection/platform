#!/usr/bin/env bash

set -o errexit
set -o nounset

echo "Running terraform operation: $OP"
echo "Terraform version: $(terraform version)"

OUTPUT_LOCATION="/app/output.json"
TOPIC_ARN=$(aws sns list-topics | jq .Topics[].TopicArn -r | grep "terraform_apply" | tail -n 1)

if [[ "$OP" == "plan" ]]
then
  echo "Running plan operation."
  /app/plan.sh
elif [[ "$OP" == "apply" ]]
then
  if [ -e terraform.plan ]
  then
    echo "Running apply operation."
    terraform apply terraform.plan

    echo "Extracting ouput to $OUTPUT_LOCATION"
    terraform output --json > "$OUTPUT_LOCATION"

    echo "Sending succesful apply notification."
    /app/notify.sh $TOPIC_ARN "$OUTPUT_LOCATION"
  else
    echo "terraform.plan not found. Have you run a plan?"
    exit 1
  fi
elif [[ "$OP" == "fmt" ]]
then
  terraform fmt
else
  echo "Unrecognised operation: $OP! Stopping."
  exit 1
fi
