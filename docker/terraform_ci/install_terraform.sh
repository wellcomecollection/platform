#!/usr/bin/env sh

set -o errexit
set -o nounset

apk update
apk add unzip wget

wget https://releases.hashicorp.com/terraform/"$TERRAFORM_VERSION"/terraform_"$TERRAFORM_VERSION"_linux_amd64.zip
unzip terraform_"$TERRAFORM_VERSION"_linux_amd64.zip
rm terraform_"$TERRAFORM_VERSION"_linux_amd64.zip

mv terraform /usr/bin/terraform

apk del unzip wget
rm -rf /var/cache/apk/*
