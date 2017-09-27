#!/usr/bin/env sh

set -o errexit
set -o nounset

echo "Getting variables from S3"
rm -f terraform.tfvars
aws s3 cp s3://platform-infra/terraform.tfvars .
