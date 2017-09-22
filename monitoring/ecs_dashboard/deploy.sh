#!/bin/sh

set -o errexit
set -o nounset

echo "Deploying dashboard to $BUCKET_NAME"

cat package.json | jq --arg homepage bar ". + {homepage: \"$PATH_PREFIX\"}" > package.json.tmp
cp package.json.tmp package.json
rm package.json.tmp

npm install
npm run build

aws s3 sync ./build "s3://$BUCKET_NAME" --acl public-read

echo "Deployment done."
