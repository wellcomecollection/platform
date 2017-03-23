#!/usr/bin/env bash

set -x
set -e
set -o errexit
set -o nounset

TF_VARS=terraform.tfvars

[ -e $TF_VARS] && rm $TF_VARS

echo "Getting variables from S3"
aws s3 cp s3://platform-infra/terraform.tfvars .

terraform init
terraform get
terraform plan

echo "Please review the above plan. If you are happy then run 'terraform apply'"
