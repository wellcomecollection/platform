#!/usr/bin/env sh

set -o errexit
set -o nounset

cat terraform_api.tfvars >> terraform.tfvars
