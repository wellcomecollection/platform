#!/usr/bin/env bash

# Fetch METS processing errors from the special error bucket.

# As the bagger processes METS, it logs the stacktrace of any error as a JSON blob
# in a special error bucket. This utility make it easier to see those errors, and
# delete errors once the problem is fixed.

# USAGE

# > .error_report.sh 
# List all available errors with a single line summary

# > .error_report.sh b12345678
# View the full stack trace for a specific b number

# > .error_report.sh delete b12345678
# Remove the S3 object for this specific error

export AWS_ACCESS_KEY_ID=''
export AWS_SECRET_ACCESS_KEY=''
export AWS_DEFAULT_REGION=''
export DROP_BUCKET_NAME_ERRORS=''

python ../src/error_report.py $1 $2