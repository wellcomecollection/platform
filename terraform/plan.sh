#!/usr/bin/env bash

set -o errexit
set -o nounset

# Name of tfvars file
TF_VARS=terraform.tfvars
RELEASE_IDS_FILE="release_ids.tfvars"


# Ensure we don't have stale variables from a previous run
rm -f $TF_VARS
rm -f $RELEASE_IDS_FILE
rm -rf releases

echo "Getting variables from S3"
aws s3 cp s3://platform-infra/terraform.tfvars .

# Check if any of our Lambda packages have changed and need to be tainted.
# Annoyingly there doesn't seem to be a way for Terraform to do this for us.
python taint_lambdas.py

# Download releases from S3
mkdir releases
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

echo "Please review the above plan. If you are happy then run 'terraform apply terraform.plan'"
