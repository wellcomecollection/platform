#!/usr/bin/env sh

set -o errexit
set -o nounset

rm -f terraform.tfvars
aws s3 cp s3://platform-infra/terraform-lambdas.tfvars terraform.tfvars
