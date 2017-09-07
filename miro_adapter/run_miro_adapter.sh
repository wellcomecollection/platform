#!/usr/bin/env sh

set -o errexit
set -o nounset

python3 miro_adapter/build_json_derivatives.py \
  --bucket="$BUCKET" \
  --src="$KEY" \
  --dst="${KEY/xml/txt}"
python3 miro_adapter/push_json_to_dynamodb.py \
  --table="$TABLE" \
  --collection="$COLLECTION" \
  --bucket="$BUCKET" \
  --json="${KEY/xml/txt}"
