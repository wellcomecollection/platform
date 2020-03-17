#!/usr/bin/env bash

set -o nounset
set -o xtrace

DEV_ROLE_ARN=${1:-}
ROOT=$(git rev-parse --show-toplevel)
SERVICE_IDS=$(find "$ROOT"/nginx/*.Dockerfile  -type f -exec basename {} \; |  cut -d '.' -f 1)

echo "*** WARNING! ***"
echo "Updating these images will result in downstream updates!"
echo "This will affect multiple public facing products (including wc.org)"
echo "*** WARNING! ***"
echo""
read -p "Press enter to continue."

for SERVICE_ID in $SERVICE_IDS
do
"$ROOT"/docker_run.py \
      --dind -- \
      wellcome/image_builder:23 \
            --project=nginx_"$SERVICE_ID" \
            --file=nginx/"${SERVICE_ID}".Dockerfile

"$ROOT"/docker_run.py \
      --aws --dind -- \
      wellcome/publish_service:86 \
        --service_id=nginx_"$SERVICE_ID" \
          --project_id=platform \
          --account_id=760097843905 \
          --region_id=eu-west-1 \
          --namespace=uk.ac.wellcome \
          --role_arn="$DEV_ROLE_ARN" \
          --label=latest
done
