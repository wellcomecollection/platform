#!/usr/bin/env bash

set -o errexit
set -o nounset

python /miro_adapter.py --table="$TABLE" --collection="$COLLECTION" --bucket="$BUCKET" --key="$KEY"
