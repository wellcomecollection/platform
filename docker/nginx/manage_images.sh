#!/usr/bin/env bash
# This script builds the Docker images for our nginx containers and
# pushes them to S3.  Takes a single argument: either BUILD or DEPLOY.

set -o errexit
set -o nounset

TASK=${1:-BUILD}
echo "Task is $TASK"

# All out nginx containers have a dedicated repository in ECR.  We need the
# URI for pushing the containers.
ECR_URI=$(
  aws ecr describe-repositories --repository-name uk.ac.wellcome/nginx | \
  jq -r '.repositories[0].repositoryUri')

# Directory name of the script itself
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"

# Log in to ECR so that we can do 'docker push'
if [[ "$TASK" == "DEPLOY" ]]
then
  $(aws ecr get-login)
fi

for conf_file in *.nginx.conf
do
  variant="$(echo "$conf_file" | tr '.' ' ' | awk '{print $1}')"
  echo "Building nginx image for $variant..."

  docker build --build-arg conf_file="$conf_file" --tag nginx_image .
  docker tag $(docker images -q nginx_image) "$ECR_URI:$variant"

  if [[ "$TASK" == "DEPLOY" ]]
  then
    docker push "$ECR_URI:$variant"
  fi
done
