#!/usr/bin/env sh

set -o errexit
set -o nounset

if [[ "${DRY_RUN:-false}" == "true" ]]
then
  python3 /elasticsearch_cleaner.py --bucket="$BUCKET" --key="$KEY" --dry-run
else
  python3 /elasticsearch_cleaner.py --bucket="$BUCKET" --key="$KEY"
fi
