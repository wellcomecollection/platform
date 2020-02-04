data "template_file" "pypi_credentials" {
  template = <<EOF
[pypi]
username=$${pypi_username}
password=$${pypi_password}
EOF

  vars = {
    pypi_username = var.pypi_username
    pypi_password = var.pypi_password
  }
}

data "archive_file" "secrets" {
  type        = "zip"
  output_path = "${path.module}/../../secrets_${var.repo_name}.zip"

  source {
    content  = data.template_file.pypi_credentials.rendered
    filename = "pypirc"
  }

  source {
    content  = tls_private_key.github_key.private_key_pem
    filename = "id_rsa"
  }

  source {
    content  = tls_private_key.github_key.public_key_openssh
    filename = "id_rsa.pub"
  }
}
