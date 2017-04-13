#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

TF_VARS=terraform.tfvars

rm -f $TF_VARS

echo "Getting variables from S3"
aws s3 cp s3://platform-infra/terraform.tfvars .

terraform init
terraform get
terraform plan -out terraform.plan

echo "Please review the above plan. If you are happy then run 'terraform apply terraform.plan'"
