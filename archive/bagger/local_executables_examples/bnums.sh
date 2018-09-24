#!/usr/bin/env bash

# Generates b numbers from the S3 keys in the METS BUCKET

# This is useful for providing a stream of valid b numbers for testing.

# USAGE:

# > . bnums.sh
# Yield all b numbers! (~250,000)

# > . bnums.sh <filter>
# Limit the b numbers returned to a filtered set, based on keys.
# The spread of b numbers is fairly even:

# > . bnums.sh 0/
# ...yields 1/11 of the total b numbers (because of the additional x checksum)

# > . bnums.sh 0/3/4/2
# ...yields about 0.01% of all the b numbers

# > . bnums.sh 0/3/4
# ...yields about 0.1% of all the b numbers

# asset sources
export METS_FILESYSTEM_ROOT=''
export METS_BUCKET_NAME=''
# aws
export AWS_ACCESS_KEY_ID=''
export AWS_SECRET_ACCESS_KEY=''
export AWS_DEFAULT_REGION=''

python ../src/mets_queuer.py $1 $2