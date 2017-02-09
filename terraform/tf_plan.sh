#!/usr/bin/env bash

KEY_NAME=$1
ADMIN_CIDR=$2

./terraform plan \
  --var "key_name=$KEY_NAME" \
  --var "admin_cidr_ingress=$ADMIN_CIDR" \
  -out=terraform.plan
