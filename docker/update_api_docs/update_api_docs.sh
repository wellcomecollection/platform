#!/usr/bin/env sh

set -o errexit
set -o nounset

SWAGGER_URL="${SWAGGER_URL:-https://api.wellcomecollection.org/catalogue/v0/swagger.json}"

echo "*** Making the IMPORT request"
curl --request PUT \
  --url "https://api.stoplight.io/v1/versions/$VERSION_ID/import" \
  --header "authorization: Bearer $API_SECRET" \
  --header "content-type: application/json" \
  --data "{
    \"url\": \"$SWAGGER_URL\",
    \"options\": {
      \"removeExtraSchemas\": true,
      \"removeExtraTextSections\": true,
      \"removeExtraTraits\": true,
      \"removeExtraEndpoints\": true
    }
  }"
echo

echo "*** Making the PUBLISH request"
curl --request POST \
  --url "https://api.stoplight.io/v1/versions/$VERSION_ID/publish" \
  --header "authorization: Bearer $API_SECRET" \
  --header "content-type: application/json" \
  --data "{}"
