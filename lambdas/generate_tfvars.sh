#!/usr/bin/env bash

set -o errexit
set -o nounset

aws s3 cp s3://platform-infra/terraform-lambdas.tfvars terraform.tfvars