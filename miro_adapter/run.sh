#!/usr/bin/env sh

set -o errexit
set -o nounset

python /miro_adapter.py --table="$TABLE" --collection="$COLLECTION" --bucket="$BUCKET" --key="$KEY"
