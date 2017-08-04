#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

# Name of tfvars file
TF_VARS=terraform.tfvars
RELEASE_IDS_FILE="release_ids.tfvars"

# Check if we're up-to-date with the current state of master.
python /app/is_up_to_date_with_master.py

# Ensure we don't have stale variables from a previous run
rm -f $TF_VARS
rm -f $RELEASE_IDS_FILE
rm -rf releases

echo "Getting variables from S3"
aws s3 cp s3://platform-infra/terraform.tfvars .

# We run two versions of the API: romulus and remus.  Travis bumps the
# release ID in S3 on every build of the API, but we don't always want to
# advance both romulus and remus.  Decide which version, if either, we want
# to bump to the latest API version.

if ! hcltool terraform.tfvars | jq --exit-status '.remus_runs_latest? == "false"' >/dev/null
then
  aws s3 cp s3://platform-infra/releases/api s3://platform-infra/releases/api_remus
  aws s3 cp s3://platform-infra/releases/nginx_api s3://platform-infra/releases/nginx_api_remus
fi

if ! hcltool terraform.tfvars | jq --exit-status '.romulus_runs_latest? == "false"' >/dev/null
then
  aws s3 cp s3://platform-infra/releases/api s3://platform-infra/releases/api_romulus
  aws s3 cp s3://platform-infra/releases/nginx_api s3://platform-infra/releases/nginx_api_romulus
fi

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

# These compiled pyc files are ephemeral, but because they're rebuilt on
# a regular basis, they can confuse Terraform about whether the Lambda code
# has changed.  They can be rebuilt easily (and automatically), so just
# delete them before running `plan`.
find /data/lambdas -name '*.pyc' -delete
find /data/lambdas -path '*.dist-info/*' -delete

terraform plan -var-file="$RELEASE_IDS_FILE" -out terraform.plan

echo "Please review the above plan. If you are happy then run 'make terraform-apply"
