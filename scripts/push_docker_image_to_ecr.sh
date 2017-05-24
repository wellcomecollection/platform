#!/usr/bin/env bash
# Push a Docker image to ECR and copy its release ID to our S3 bucket.

set -o errexit
set -o nounset

$(aws ecr get-login --no-include-email)
docker push "$TAG"
echo "New container image is $RELEASE_ID"

echo "$RELEASE_ID" | aws s3 cp - "s3://$CONFIG_BUCKET/releases/$PROJECT"

exit 0
