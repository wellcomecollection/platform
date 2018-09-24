#!/usr/bin/env bash

# Run the bagging process on the local machine (i.e., not from a queue)

# USAGE
# . local_bag_all.sh <filter> <bag|no-bag>
# e.g.,  . local_bag_all.sh x/1/2 no-bag

# The filter limits the b numbers returned to a filtered set, based on keys.
# The spread of b numbers is fairly even:

# > . local_bag_all.sh 0/ no-bag
# ...yields 1/11 of the total b numbers (because of the additional x checksum)
# ...processes mets only.

# > . local_bag_all.sh 0/3/4/2 bag
# ...yields about 0.01% of all the b numbers

# > . local_bag_all.sh 0/3/4 bag
# ...yields about 0.1% of all the b numbers

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

python ../src/local_bag_all.py $1 $2