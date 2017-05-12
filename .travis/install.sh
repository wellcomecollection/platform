#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

# This is needed for the Elasticsearch docker container to start.
# See https://github.com/travis-ci/travis-ci/issues/6534
sudo sysctl -w vm.max_map_count=262144

# Install Docker on Travis.  Merely exposing Docker through the Travis
# settings is insufficient, because the docker-compose tests we use are
# unable to see it in that case.
curl -sSL "https://get.docker.com/gpg" | sudo -E apt-key add -
echo "deb https://apt.dockerproject.org/repo ubuntu-trusty main" | sudo tee -a /etc/apt/sources.list
sudo apt-get update
sudo apt-get -o Dpkg::Options::="--force-confdef" -o Dpkg::Options::="--force-confold" --assume-yes install docker-engine
docker version

# Install the AWS tools so we can log in to ECR
pip install --upgrade --user awscli

# Load any Docker images from the cache
mkdir -p ~/.cache/docker
find ~/.cache/docker -name '*.tar' -exec docker load --input "{}" \;
