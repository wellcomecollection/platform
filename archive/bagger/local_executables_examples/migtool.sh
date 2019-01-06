#!/usr/bin/env bash

# TODO

# Dynamo
export DYNAMO_TABLE=''
# asset sources
export METS_FILESYSTEM_ROOT=''
export METS_BUCKET_NAME=''

export DROP_BUCKET_NAME=''
# aws
export AWS_ACCESS_KEY_ID=''
export AWS_SECRET_ACCESS_KEY=''
export AWS_DEFAULT_REGION=''

export WELLCOME_API_CLIENT_ID=''
export WELLCOME_API_CLIENT_SECRET=''
export WELLCOME_API_TOKEN_ENDPOINT=''
export STORAGE_API_BAGS=''
export STORAGE_API_INGESTS=''


python ../src/migration_tools.py "$@"