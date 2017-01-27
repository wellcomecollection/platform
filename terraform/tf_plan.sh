#!/usr/bin/env bash

KEY_NAME=$1
ADMIN_CIDR=$2
CONTAINER_URL=$3
INFRA_BUCKET=$4
BUILD_SHA1=$5

set -e

./terraform plan \
  --var "key_name=$KEY_NAME" \
  --var "admin_cidr_ingress=$ADMIN_CIDR" \
  --var "platform_api_container_url=$CONTAINER_URL" \
  -out=terraform.plan

aws s3 cp terraform.plan s3://$INFRA_BUCKET/$BUILD_SHA1/terraform.plan
