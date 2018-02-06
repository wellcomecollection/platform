#!/usr/bin/env bash
# Build an image for testing a Lambda.  Prints the name of the new image.
#
# Because some of our Lambdas have "requirements.txt" files, we want
# to install those dependencies in an image once, then not reinstall them
# again.  The first test run is a bit slow, but later runs should be faster.
#
# This script builds an "intermediate" image, that derives from our standard
# "test_lambda" image, which has any necessary requirements installed.
#
# Usage:
#
#   - $1: Path to the Lambda's "src" dir, relative to the repo root.
#

set -o errexit
set -o nounset

SRC="$1/src"
LABEL=$(basename $1)


# Name of the new Docker image
DOCKER_IMAGE="wellcome/test_lambda_$LABEL"

# Root of the repo
ROOT=$(git rev-parse --show-toplevel)

# Marker which indicates the image has been created
MARKER=$ROOT/.docker/lambda_test_$LABEL



# If we don't already have the image, pull it now.
if ! docker inspect --type=image wellcome/test_lambda >/dev/null 2>&1
then
  docker pull wellcome/test_lambda:latest
fi

# If a requirements.txt file exists, we need to check if it's more up-to-date
# than the existing Lambda image, and rebuild if so.
if [[ -f $ROOT/$SRC/requirements.txt ]]
then
  if [[ ! -f $MARKER || $MARKER -ot $ROOT/$SRC/requirements.txt ]]
  then
    DOCKERFILE=$SRC/.Dockerfile
    echo "FROM wellcome/test_lambda:latest"              > $DOCKERFILE
    echo "COPY requirements.txt /"                      >> $DOCKERFILE
    echo "RUN pip3 install -r /requirements.txt"        >> $DOCKERFILE

    if [[ -f $ROOT/$SRC/test_requirements.txt ]]
    then
      echo "COPY test_requirements.txt /"               >> $DOCKERFILE
      echo "RUN pip3 install -r /test_requirements.txt" >> $DOCKERFILE
    fi

    docker build --tag $DOCKER_IMAGE --file $DOCKERFILE $SRC
  fi
else
  docker tag wellcome/test_lambda $DOCKER_IMAGE
fi

mkdir -p $ROOT/.docker
touch $MARKER
