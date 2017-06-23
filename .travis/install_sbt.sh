#!/usr/bin/env bash
# This script runs the sbt-specific install steps.

set -o errexit
set -o nounset

# This is needed for the Elasticsearch docker container to start.
# See https://github.com/travis-ci/travis-ci/issues/6534
sudo sysctl -w vm.max_map_count=262144

# Install the AWS tools so we can log in to ECR
pip install --upgrade --user awscli
