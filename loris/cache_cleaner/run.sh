#!/usr/bin/env sh

set -o errexit
set -o nounset

python3 /cache_cleaner.py --path=/data --max-age="$MAX_AGE" --max-size="$MAX_SIZE" --force
