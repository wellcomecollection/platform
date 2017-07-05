#!/usr/bin/env bash
# This script builds the Docker images for our nginx containers and
# pushes them to S3.  Takes a single argument: either BUILD or DEPLOY.

set -o errexit
set -o nounset

TASK=${1:-BUILD}
echo "*** Task is $TASK"

export AWS_DEFAULT_REGION=${AWS_DEFAULT_REGION:-eu-west-1}
export CONFIG_BUCKET=${CONFIG_BUCKET:-platform-infra}

# Root of the repository
ROOT=$(git rev-parse --show-toplevel)

# Directory name of the script itself
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"

# Root of the repository
ROOT="$(git rev-parse HEAD)"

# Log in to ECR so that we can do 'docker push'
if [[ "$TASK" == "DEPLOY" ]]
then
  $(aws ecr get-login)
fi

for conf_file in *.nginx.conf
do
  variant="$(echo "$conf_file" | tr '.' ' ' | awk '{print $1}')"
  echo "*** Building nginx image for $variant..."

  # All out nginx containers have a dedicated repository in ECR.  We need the
  # URI for pushing the containers.
  export ECR_URI=$(
    aws ecr describe-repositories --repository-name "uk.ac.wellcome/nginx_$variant" | \
    jq -r '.repositories[0].repositoryUri')
  echo "*** ECR_URI is $ECR_URI"

  if [[ "$ECR_URI" == "" ]]
  then
    echo "*** Failed to read ECR repo information" >&2
    exit 1
  fi

  # Construct the tag used for the image
  RELEASE_ID="$(git rev-parse HEAD)"
  TAG="nginx:$RELEASE_ID"
  echo "*** Image will be tagged $TAG"

  docker build --build-arg conf_file="$conf_file" --tag "$TAG" .

  mkdir -p "$ROOT/.releases"
  echo "$RELEASE_ID" > "$ROOT/.releases/nginx_$variant"

  if [[ "$TASK" == "DEPLOY" ]]
  then
    $ROOT/scripts/deploy_docker_to_aws.py --docker-image="$TAG" --infra-bucket="$CONFIG_BUCKET"
  fi
  echo
done
