#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

# Name of tfvars file
TF_VARS=terraform.tfvars
RELEASE_IDS_FILE="release_ids.tfvars"

# Check if we're up-to-date with the current state of master.
python3 /app/is_up_to_date_with_master.py

# Ensure we don't have stale variables from a previous run
rm -f $TF_VARS
rm -f $RELEASE_IDS_FILE
rm -rf releases

echo "Getting variables from S3"
aws s3 cp s3://platform-infra/terraform.tfvars .

# Download releases from S3
mkdir -p releases
aws s3 cp s3://platform-infra/releases releases --recursive

# Build a tfvars file containing the release ids
echo "release_ids = {" > "$RELEASE_IDS_FILE"
for f in releases/*;
do
  echo "Processing $f: $(cat $f)"
  echo "  $(basename $f) = \"$(cat $f)\"" >> "$RELEASE_IDS_FILE"
done
echo "}" >> "$RELEASE_IDS_FILE"

terraform init
terraform get

terraform plan -var-file="$RELEASE_IDS_FILE" -out terraform.plan

echo "Please review the above plan. If you are happy then run 'make terraform-apply"
