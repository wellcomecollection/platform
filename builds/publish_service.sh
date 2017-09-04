#!/usr/bin/env bash

set -o errexit
set -o nounset

docker run --tty \
	--volume /var/run/docker.sock:/var/run/docker.sock \
	--volume $(pwd):/repo \
	--volume ~/.aws:/root/.aws \
	publish_service_to_aws --project="$PROJECT" --infra-bucket="$INFRA_BUCKET"
