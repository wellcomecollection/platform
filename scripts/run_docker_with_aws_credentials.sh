#!/usr/bin/env bash

if [[  -z ${AWS_ACCESS_KEY_ID+x} ]]
    then
        docker run -v $HOME/.aws:/root/.aws "$@"
    else
        docker run -e AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID -e AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY "$@"
fi
