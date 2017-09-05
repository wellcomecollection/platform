#!/usr/bin/env bash

set -o errexit
set -o nounset

FILE="${FILE-}"
VARIANT="${VARIANT-}"

if [[ ! -z "$FILE" ]]
then
  docker run --tty \
    --volume /var/run/docker.sock:/var/run/docker.sock \
    --volume $(pwd):/repo \
    image_builder --project="$PROJECT" --file="$FILE"
elif [[ ! -z "$VARIANT" ]]
then
  docker run --tty \
    --volume /var/run/docker.sock:/var/run/docker.sock \
    --volume $(pwd):/repo \
    image_builder --project="$PROJECT" --variant="$VARIANT"
else
  docker run --tty \
    --volume /var/run/docker.sock:/var/run/docker.sock \
    --volume $(pwd):/repo \
    image_builder --project="$PROJECT"
fi
