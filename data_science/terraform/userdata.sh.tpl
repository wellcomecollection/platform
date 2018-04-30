#!/bin/bash

# Create jupyter user
adduser ${notebook_user} --gecos "" --disabled-password

# Write config file
mkdir /home/${notebook_user}/.jupyter
cat << EOF > /home/${notebook_user}/.jupyter/jupyter_notebook_config.py

c.NotebookApp.notebook_dir = u"/home/${notebook_user}/"
c.NotebookApp.ip = "*"
c.NotebookApp.port = "${notebook_port}"
c.NotebookApp.open_browser = False
c.NotebookApp.password = u'${hashed_password}''

EOF

# Start notebook server
runuser -l ${notebook_user} -c '/home/ubuntu/anaconda3/bin/jupyter notebook'