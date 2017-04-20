#!/usr/bin/env bash
# This script grabs a few containers and caches them locally for Circle.

set -o errexit
set -o nounset
set -o verbose

IMAGES="peopleperhour/dynamodb alicefuzier/fake-sns s12v/elasticmq"

mkdir -p ~/.ivy2/docker
for img in $IMAGES
do
    out_name=$(echo "$img" | tr '/' '_')
    if [[ -f ~/.ivy2/docker/$out_name.tar ]]
    then
        docker load < ~/.ivy2/docker/$out_name.tar
    fi

    docker pull $img
    docker save $img > ~/.ivy2/docker/$out_name.tar
done
