#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o xtrace

# Create jupyter user
adduser ${notebook_user} --gecos "" --disabled-password

# Write config file
mkdir /home/${notebook_user}/.jupyter
cat << EOF > /home/${notebook_user}/.jupyter/jupyter_notebook_config.py
${jupyter_notebook_config}
EOF

# Select the version of pip for our default environment.
PIP=/home/ubuntu/anaconda3/envs/${default_environment}/bin/pip

cat << EOF > /home/${notebook_user}/requirements.txt
${requirements}
EOF

$PIP install --upgrade pip
$PIP install --requirement /home/${notebook_user}/requirements.txt > /home/${notebook_user}/pip_install.log 2>&1

# Install s3contents.  This needs to be installed in the top-level anaconda
# environment, or it won't be available to Jupyter, and it will fail to start.
/home/ubuntu/anaconda3/bin/pip install s3contents

# Set up the EFS mount.
#
# Install utilities for creating an EFS mount on Ubuntu.
# Based on README instructions https://github.com/aws/efs-utils
git clone https://github.com/aws/efs-utils
pushd efs-utils
  git checkout 7ba8784

  sudo apt-get update
  sudo apt-get --yes install binutils
  ./build-deb.sh
  sudo apt-get --yes install ./build/amazon-efs-utils*deb

  # See https://docs.aws.amazon.com/efs/latest/ug/mounting-fs.html
  sudo mkdir -p /mnt/efs
  sudo mount -t efs ${efs_mount_id}:/ /mnt/efs

  # Ensure the Jupyter user can actually write to this file!
  sudo chown -R ${notebook_user}:${notebook_user} /mnt/efs
popd

# Start notebook server
runuser --login ${notebook_user} --command '/home/ubuntu/anaconda3/bin/jupyter notebook'
