#!/usr/bin/env bash

KEY_NAME=$1
ADMIN_CIDR=$2
INFRA_BUCKET=$3
BUILD_SHA1=$5

./terraform plan \
  --var "key_name=$KEY_NAME" \
  --var "admin_cidr_ingress=$ADMIN_CIDR" \
  --var "platform_api_container_url=$CONTAINER_URL" \
  -out=terraform.plan
