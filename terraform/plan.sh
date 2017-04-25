#!/usr/bin/env bash

set -o errexit
set -o nounset

# Name of tfvars file
TF_VARS=terraform.tfvars

# Are we running in Travis?  This environment variable is set to "true"
# in the Travis environment.
TRAVIS=${TRAVIS:-false}


# Ensure we don't have stale variables from a previous run
rm -f $TF_VARS

echo "Getting variables from S3"
aws s3 cp s3://platform-infra/terraform.tfvars .

# This uses the ECR API to get the image ID of the most recently pushed
# image in our ECR repo.  This uses the API repo because every change pushes
# a new image for _every_ application.
latest_image_id="$(
    aws ecr describe-images --repository-name uk.ac.wellcome/api |
    jq -r '.imageDetails | max_by(.imagePushedAt) | .imageTags[0]')"

if ! grep -q "$latest_image_id" terraform.tfvars
then
    echo ""
    echo "WARNING! You may be about to downgrade to an older version of the API."
    echo "The latest ECR image ID is $latest_image_id."
    echo "This does not match the image ID in your terraform.tfvars file."
    echo ""
    echo "To continue, type 'downgrade'."
    read resp
    if [[ "$resp" != "downgrade" ]]
    then
        echo "Aborting..."
        exit 1
    fi
fi

terraform init
terraform get

if [[ "$TRAVIS" == "true" ]]
then
    echo "Running in Travis, disabling push of remote state."
    mv terraform.tf terraform.tf.bak
    terraform init -force-copy -lock=false
fi

terraform plan -out terraform.plan

echo "Please review the above plan. If you are happy then run 'terraform apply terraform.plan'"
