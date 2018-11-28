#!/usr/bin/env bash
# Used to re-PUT a window in Amazon S3.  This is useful for testing the
# pipeline, if you want to re-run a window through the transformer.
#
# Usage: reput_window.sh <WINDOW>
#
# Example: reput_window.sh records_bibs/2018-01-13T03-16-21.028291+00-00__2018-01-13T03-46-21.028291+00-00

set -o errexit
set -o nounset

WINDOW="$1"

pushd $(mktemp -d)
  aws s3 cp --recursive s3://wellcomecollection-sierra-adapter-data/$WINDOW .
  aws s3 cp --recursive . s3://wellcomecollection-sierra-adapter-data/$WINDOW
popd
