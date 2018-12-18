#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o xtrace


# We're going to use the Message Of The Day (MOTD) to report the progress
# of this bootstrap script.  First remove a bunch of stuff from the default
# MOTD to make it a bit cleaner.
rm -f /etc/update-motd.d/00-header
rm -f /etc/update-motd.d/51-cloudguest
rm -f /etc/update-motd.d/9*

# This script will print "BOOTSTRAP COMPLETE/PENDING" when you first log
# on to the box, depending on whether this script has completed.
#
# It relies on the fact that this script is written on the box to
#
#     /var/lib/cloud/instance/scripts/part-001
#
# which seems to be true, but is based on anecdotal evidence.  This has
# been consistent so far, but we can't be sure it's true!
#
BOOTSTRAP_COMPLETE_PATH="/usr/local/bootstrap_complete"

cat << EOF > /etc/update-motd.d/99-bootstrap-motd
#!/bin/bash

echo ""

ps -eaf | grep "bash /var/lib/cloud/instance/scripts/part-001" | grep --quiet --invert-match grep


if (( \$? == 0 ))
then
  echo -e "\e[33m*** BOOTSTRAP RUNNING ***"
  echo ""
  echo "Follow the bootstrap script logs for progress info:"
  echo ""
  echo -e "$ tail -f /var/log/cloud-init-output.log\e[0m"
else
  if [[ -f "$BOOTSTRAP_COMPLETE_PATH" ]]
  then
    echo -e "\e[32m*** BOOTSTRAP COMPLETE ***\e[0m"
  else
    echo -e "\e[31m*** BOOTSTRAP FAILED ***"
    echo ""
    echo "Check the bootstrap script logs for more information:"
    echo ""
    echo -e "$ cat /var/log/cloud-init-output.log\e[0m"
  fi
fi

echo ""
EOF

chmod +x /etc/update-motd.d/99-bootstrap-motd



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
popd

# Mark this script as complete.
touch "$BOOTSTRAP_COMPLETE_PATH"
wall "*** BOOTSTRAP COMPLETE ***"

# Start notebook server
runuser --login ${notebook_user} --command '/home/ubuntu/anaconda3/bin/jupyter notebook'
