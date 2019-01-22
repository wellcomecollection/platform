#!/usr/bin/env bash

# Listens to an SQS queue for b numbers to bag.
# This is for running locally, but usually this will be run on AWS in a container.

# asset sources
export METS_FILESYSTEM_ROOT=''
export METS_BUCKET_NAME=''
export READ_METS_FROM_FILESHARE=''
export WORKING_DIRECTORY=''
export DROP_BUCKET_NAME=''
export DROP_BUCKET_NAME_METS_ONLY=''
export DROP_BUCKET_NAME_ERRORS=''
export CURRENT_PRESERVATION_BUCKET=''
export DLCS_SOURCE_BUCKET=''
export BAGGING_QUEUE=''

# Dynamo
export DYNAMO_TABLE=''

# aws
export AWS_ACCESS_KEY_ID=''
export AWS_SECRET_ACCESS_KEY=''
export AWS_DEFAULT_REGION=''

# DLCS config
export DLCS_ENTRY=''
export DLCS_API_KEY=''
export DLCS_API_SECRET=''
export DLCS_CUSTOMER_ID=''
export DLCS_SPACE=''

# DDS credentials
export DDS_API_KEY=''
export DDS_API_SECRET=''
export DDS_ASSET_PREFIX=''

python ../src/main.py