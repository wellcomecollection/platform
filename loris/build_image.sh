#!/usr/bin/env bash
# Build the Docker image for Loris and push it to ECR.

set -o errexit
set -o nounset

# Parse arguments
if (( $# == 1 )); then
    ECR_URI="$1"
else
    echo "Usage: build_images.sh <ECR_URI>" >&2
    exit 1
fi

# Directory name of the script itself
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"

# Log in to ECR so that we can do 'docker push'
$(aws ecr get-login)

docker build --tag loris_image .
docker tag $(docker images -q loris_image) "$ECR_URI/uk.ac.wellcome/loris:latest"
docker push "$ECR_URI/uk.ac.wellcome/loris:latest"
