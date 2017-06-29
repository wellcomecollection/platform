#!/usr/bin/env bash
# This script builds the Docker images for our nginx containers and
# pushes them to S3.  Take a single argument: the URI to our ECR.

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

for conf_file in *.nginx.conf; do
    variant="$(echo "$conf_file" | tr '.' ' ' | awk '{print $1}')"
    echo $variant
    echo "Building nginx image for $variant..."
    docker build --build-arg conf_file="$conf_file" --tag nginx_image .
    docker tag $(docker images -q nginx_image) "$ECR_URI/uk.ac.wellcome/nginx:$variant"
    docker push "$ECR_URI/uk.ac.wellcome/nginx:$variant"
done
