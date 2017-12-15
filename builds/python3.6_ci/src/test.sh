#!/usr/bin/env sh

set -o errexit
set -o nounset
set -o verbose

FIND_MATCH_PATHS=$1

echo "FIND_MATCH_PATHS=$FIND_MATCH_PATHS"

# Run tests
find $1 -maxdepth 1 -name "test_*.py" | xargs py.test
