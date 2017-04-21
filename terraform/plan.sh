#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

TF_VARS=terraform.tfvars

rm -f $TF_VARS

echo "Getting variables from S3"
aws s3 cp s3://platform-infra/terraform.tfvars .

latest_image_id="$(aws ecr describe-images --repository-name uk.ac.wellcome/api | jq -r '.imageDetails | max_by(.imagePushedAt) | .imageTags[0]')"
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

# When running in Travis, move the config telling us about the remote
# state file.  Otherwise, Travis will attempt to update the state file,
# which is:
#  1) Not something we want to do
#  2) Not allowed with its IAM permissions
if [[ ! -z "$TRAVIS" && "$TRAVIS" == "true" ]]
then
    mv terraform.tf terraform.tf.bak
    terraform init -force-copy
fi

terraform plan -out terraform.plan

echo "Please review the above plan. If you are happy then run 'terraform apply terraform.plan'"
