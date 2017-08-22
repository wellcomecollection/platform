#!/usr/bin/env sh

set -o errexit
set -o nounset

aws s3 cp s3://platform-infra/config/prod/ingestor.ini ingestor_config.ini

# Create the HTTP auth file required by elasticdump
username=$(awk -F "=" '/es.username/ {print $2}' ingestor_config.ini)
password=$(awk -F "=" '/es.password/ {print $2}' ingestor_config.ini)
echo "user=$username"      > auth.ini
echo "password=$password" >> auth.ini

# Now run the elasticdump tool and pull it down
hostname=$(awk -F "=" '/es.host/ {print $2}' ingestor_config.ini)
outfile="dump_$(date +"%Y-%m-%d_%H-%M-%S")_$INDEX.txt"
elasticdump --input="https://$hostname/$INDEX" --output="$outfile" --httpAuthFile=auth.ini

# And copy it back up to S3
aws s3 cp "$outfile" "s3://platform-infra/elasticdump/$outfile"
