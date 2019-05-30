c = get_config()

c.NotebookApp.notebook_dir = u"/mnt/ebs"
c.NotebookApp.ip = "*"
c.NotebookApp.port = ${notebook_port}
c.NotebookApp.open_browser = False
c.NotebookApp.password = u'${hashed_password}'