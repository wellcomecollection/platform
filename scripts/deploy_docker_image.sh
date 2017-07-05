#!/usr/bin/env bash
# This script:
#
#   * prepends our ECR URI to the Docker image tag
#   * pushes the Docker image to an ECR repo
#   * stores the image tag in S3
#

set -o errexit
set -o nounset


# Parse command-line arguments.  It would be nicer to do this with flags,
# e.g. --ecr-repo-name=<ECR_REPO_NAME>, but doing that in bash is more of
# a faff than I care to do.
if (( $# != 5 ))
then
  echo "Usage: deploy_docker_image.sh <ECR_REPO_NAME> <IMAGE_ID> <CONFIG_BUCKET> <PROJECT> <RELEASE_ID>" >&2
  exit 1
fi


ECR_REPO_NAME="$1"
IMAGE_ID="$2"
CONFIG_BUCKET="$3"
RELEASE_ID="$5"
PROJECT="$4"


# All our nginx containers have a dedicated repo in ECR.  We use the repo name
# to identify the repo URL.
ECR_URI=$(
  aws ecr describe-repositories --repository-name "$ECR_REPO_NAME" |
  jq -r '.repositories[0].repositoryUri'
)
echo "*** ECR_URI is $ECR_URI"

if [[ "$ECR_URI" == "" ]]
then
  echo "*** Failed to read ECR repo information" >&2
  exit 1
fi


# Log in to ECR so we can do 'docker push' to an ECR repo.
$(aws ecr get-login)


# Actually deploy the image: prepend the ECR URI to the image tag, push to ECR,
# then deleted the ECR-tagged image.  The image ID will be of the form
#
#    image_name:image_tag
#
# and we want to replace the image name with our new ECR_URI.  The sed command
# throws away everything up to the colon in the passed-in image ID.
#
NEW_IMAGE_ID="$ECR_URI:$(echo "$IMAGE_ID" | sed 's/.*://')"
echo "*** Image ID in ECR will be $NEW_IMAGE_ID"

echo "*** Pushing image to ECR"
docker tag "$IMAGE_ID" "$NEW_IMAGE_ID"
trap "docker rmi '$NEW_IMAGE_ID'" INT TERM EXIT
docker push "$NEW_IMAGE_ID"


# Finally, upload the release ID string to S3.
echo "*** Uploading release ID to S3"
echo "$RELEASE_ID" | aws s3 cp - "s3://$CONFIG_BUCKET/releases/$PROJECT"
