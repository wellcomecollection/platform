#!/usr/bin/env bash

# Name of tfvars file
TF_VARS=terraform.tfvars
RELEASE_IDS_FILE="release_ids.tfvars"

# Ensure we don't have stale variables from a previous run
rm -f $TF_VARS
rm -rf releases

echo "Getting variables from S3"
aws s3 cp s3://platform-infra/terraform.tfvars .

# Download releases from S3
mkdir -p releases
aws s3 cp s3://platform-infra/releases releases --recursive

# Build a tfvars file containing the release ids
echo "release_ids = {" >> "$TF_VARS"
for f in releases/*;
do
  echo "Processing $f: $(cat $f)"
  echo "  $(basename $f) = \"$(cat $f)\"" >> "$TF_VARS"
done
echo "}" >> "$TF_VARS"
