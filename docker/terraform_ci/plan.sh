#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

# Check if we're up-to-date with the current state of master.
# python3 /app/is_up_to_date_with_master.py

# Run the generate_tfvars hook script to prepare tfvars
if [ -f generate_tfvars.sh ];
then
    ./generate_tfvars.sh
fi

terraform init
terraform get
terraform plan -out terraform.plan

echo "Please review the above plan. If you are happy then run 'make terraform-apply"
