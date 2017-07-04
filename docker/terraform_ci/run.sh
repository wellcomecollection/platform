#!/usr/bin/env bash

set -o errexit
set -o nounset

export OP="${OP:-plan}"

if [[ "$OP" == "plan" ]]
then
  echo "Running plan operation."
  ./plan.sh
elif [[ "$OP" == "apply" ]]
then
  if [ -e terraform.plan ]
  then
    echo "Running apply operation."
    terraform apply terraform.plan
  else
    echo "terraform.plan not found. Have you run a plan?"
    exit 1
  fi
else
  echo "Unrecognised operation: $OP! Stopping."
  exit 1
fi
