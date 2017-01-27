#!/usr/bin/env bash

DEPLOY_ENV=$1
IMAGE_URL=$2

set +e

aws s3 cp s3://platform-infra/config/$DEPLOY_ENV/platform.conf .

docker pull $IMAGE_URL
CONTAINER_ID=`docker run -d $IMAGE_URL`
docker cp platform.conf $CONTAINER_ID:/etc/wellcome/platform.conf
docker commit $CONTAINER_ID ${IMAGE_URL}_$DEPLOY_ENV
docker kill $CONTAINER_ID
docker push ${IMAGE_URL}_$DEPLOY_ENV

echo ${IMAGE_URL}_$DEPLOY_ENV > image.url

set -e
