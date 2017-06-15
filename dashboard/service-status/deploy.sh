#!/bin/sh
# Usage: ./deploy.sh my-bucket https://s3-eu-west-1.amazonaws.com/my-bucket

set -o errexit
set -o nounset
set -o xtrace

BUCKET_NAME=$1
PATH_PREFIX=$2

echo "Deploying dashboard to $BUCKET_NAME"

cat package.json | jq --arg homepage bar ". + {homepage: \"$PATH_PREFIX\"}" > package.json.tmp
cp package.json.tmp package.json
rm package.json.tmp

npm run build

aws s3 sync ./build "s3://$BUCKET_NAME" --acl public-read

echo "Deployment done."

exit 0
