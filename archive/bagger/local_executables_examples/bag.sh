#!/usr/bin/env bash

# Creates bagit bags by processing METS files and collecting assets from their various locations.
# Can run in mets-only mode, where no bags are created and no I/O operations happen,
# other than on METS files.

# USAGE

# > . bag.sh clean
# Delete contents of working directory.

# > . bag.sh b12345678 <bag|no-bag>
# Bag a b number. If 'no-bag', process METS only.

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

python ../src/bagger.py $1 $2