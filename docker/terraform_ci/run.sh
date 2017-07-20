#!/usr/bin/env bash

set -o errexit
set -o nounset

export OP="${OP:-plan}"

echo "Running terraform operation: $OP"
echo "Terraform version: $(terraform version)"

OUTPUT_LOCATION="/app/output.json"

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
    /app/notify.sh terraform_apply "$OUTPUT_LOCATION"
  else
    echo "terraform.plan not found. Have you run a plan?"
    exit 1
  fi
else
  echo "Unrecognised operation: $OP! Stopping."
  exit 1
fi
