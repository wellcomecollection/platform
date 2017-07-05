#!/usr/bin/env bash

set -o errexit
set -o nounset

rm -f terraform.outputs.json

terraform apply terraform.plan -json | terraform.outputs.json

aws s3 cp terraform.outputs.json s3://platform-infra/terraform.outputs.json