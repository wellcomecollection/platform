#!/usr/bin/env bash
# This script grabs a few containers and caches them locally for Circle.

set -o errexit
set -o nounset
set -o verbose

IMAGES="peopleperhour/dynamodb alicefuzier/fake-sns s12v/elasticmq"

for img in $IMAGES
do
    if [[ -f ~/.ivy2/docker/$img.tar ]]
    then
        docker load < ~/.ivy2/docker/$img.tar
    fi

    docker pull $img
    docker save $img > ~/.ivy2/docker/$img.tar
done
