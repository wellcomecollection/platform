#!/usr/bin/env bash

set -o errexit
set -o nounset

# Create jupyter user
adduser ${notebook_user} --gecos "" --disabled-password

# Write config file
mkdir /home/${notebook_user}/.jupyter
cat << EOF > /home/${notebook_user}/.jupyter/jupyter_notebook_config.py

from s3contents import S3ContentsManager

c = get_config()

# Tell Jupyter to use S3ContentsManager for all storage.
c.NotebookApp.contents_manager_class = S3ContentsManager
c.S3ContentsManager.bucket = "${bucket_name}"

c.NotebookApp.notebook_dir = u"/home/${notebook_user}/"
c.NotebookApp.ip = "*"
c.NotebookApp.port = ${notebook_port}
c.NotebookApp.open_browser = False
c.NotebookApp.password = u'${hashed_password}'

EOF

# Upgrade and install latest versions of modules
/home/ubuntu/anaconda3/bin/pip install --upgrade pip
/home/ubuntu/anaconda3/bin/pip install \
            pillow==5.1.0 \
            seaborn==0.8.1 \
            scikit-learn==0.19.1 \
            tqdm==4.19.7

# Install s3contents
/home/ubuntu/anaconda3/bin/pip install s3contents==0.2.2

# Start notebook server
runuser --login ${notebook_user} --command '/home/ubuntu/anaconda3/bin/jupyter notebook'
