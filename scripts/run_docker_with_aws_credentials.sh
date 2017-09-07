#!/usr/bin/env bash

if [[  -z ${AWS_ACCESS_KEY_ID+x} ]]
    then
        echo "AWS_ACCESS_KEY_ID undefined, using ~/.aws credentials"
        docker run -v $HOME/.aws:/root/.aws "$@"
    else
        echo "AWS_ACCESS_KEY_ID defined, using environment variables"
        docker run \
        -e AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID \
        -e AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY \
        -e AWS_REGION=$AWS_REGION \
        -e AWS_DEFAULT_REGION=$AWS_DEFAULT_REGION "$@"
fi
