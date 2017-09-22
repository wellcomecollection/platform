#!/usr/bin/env bash

set -o errexit
set -o nounset

ROOT=$(git rev-parse --show-toplevel)

$ROOT/builds/run_docker.py --aws --dind -- \
	publish_service_to_aws --project="$PROJECT" --infra-bucket="$INFRA_BUCKET"
