#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o verbose

# Start by installing Terraform
repo_dir=$(pwd)
cd /tmp
curl https://releases.hashicorp.com/terraform/0.9.3/terraform_0.9.3_linux_amd64.zip -o terraform.zip
unzip -o terraform.zip
sudo mv terraform /usr/bin/terraform
echo "Terraform available at $(which terraform)"

cd $repo_dir/terraform
./plan.sh
