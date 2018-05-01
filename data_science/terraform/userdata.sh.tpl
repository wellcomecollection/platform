#!/usr/bin/env bash

set -o errexit
set -o nounset

# Create jupyter user
adduser ${notebook_user} --gecos "" --disabled-password

# Write config file
mkdir /home/${notebook_user}/.jupyter
cat << EOF > /home/${notebook_user}/.jupyter/jupyter_notebook_config.py

c.NotebookApp.notebook_dir = u"/home/${notebook_user}/"
c.NotebookApp.ip = "*"
c.NotebookApp.port = ${notebook_port}
c.NotebookApp.open_browser = False
c.NotebookApp.password = u'${hashed_password}'

EOF

# Start notebook server
runuser --login ${notebook_user} --command '/home/ubuntu/anaconda3/bin/jupyter notebook'
