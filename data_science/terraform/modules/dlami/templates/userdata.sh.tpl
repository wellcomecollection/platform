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

# Write config file
mkdir -p /home/ec2-user/.jupyter
cat << EOF > /home/ec2-user/.jupyter/jupyter_notebook_config.py
${jupyter_notebook_config}
EOF

# Select the version of pip for our default environment.
PIP=${anaconda_path}/envs/${default_environment}/bin/pip

cat << EOF > /home/ec2-user/requirements.txt
${requirements}
EOF

#$PIP install --upgrade pip
# commenting out the pip upgrade for the reasons below as well
# adding --ignore-installed to get around the pre-installed clashes
# https://stackoverflow.com/questions/42020151/cannot-remove-entries-from-nonexistent-file
$PIP install --requirement /home/ec2-user/requirements.txt --ignore-installed > /home/ec2-user/pip_install.log 2>&1

# Set up the EFS mount.
yum install -y amazon-efs-utils
mkdir -p /mnt/efs
mount -t efs ${efs_mount_id}:/ /mnt/efs

# Set up the EBS mount.
mkdir -p /mnt/ebs
mount /dev/sdh /mnt/ebs
chown ec2-user:ec2-user /mnt/ebs

# Start notebook servercat
sudo -u ec2-user nohup ${anaconda_path}/bin/jupyter notebook &

# Mark this script as complete.
touch "$BOOTSTRAP_COMPLETE_PATH"
wall "*** BOOTSTRAP COMPLETE ***"