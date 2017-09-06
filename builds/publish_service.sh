#!/usr/bin/env bash

set -o errexit
set -o nounset

ROOT=$(git rev-parse --show-toplevel)

$ROOT/scripts/run_docker_with_aws_credentials.sh --tty \
	--volume /var/run/docker.sock:/var/run/docker.sock \
	--volume $ROOT:/repo \
	publish_service_to_aws --project="$PROJECT" --infra-bucket="$INFRA_BUCKET"
