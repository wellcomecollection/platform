#!/usr/bin/env bash

INFRA_BUCKET=$1

set +e

aws s3 cp s3://$INFRA_BUCKET/terraform_0.8.4_linux_amd64.zip .
unzip terraform_0.8.4_linux_amd64.zip

./terraform remote config \
  -backend=S3 \
  -backend-config="bucket=$INFRA_BUCKET" \
  -backend-config="key=terraform.tfstate" \
  -backend-config="region=eu-west-1"

./terraform get

set -e
