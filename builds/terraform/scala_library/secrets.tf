data "template_file" "aws_credentials" {
  template = <<EOF
[default]
aws_access_key_id=$${access_key_id}
aws_secret_access_key=$${secret_access_key}
EOF

  vars {
    access_key_id     = "${aws_iam_access_key.travis_ci.id}"
    secret_access_key = "${aws_iam_access_key.travis_ci.secret}"
  }
}

# The docs for this resource say pretty clearly:
#
#     Usage in production deployments is not recommended
#
# Since this key is only used to grant push access to a couple of
# non-critical repos, I'll take the risk.

resource "tls_private_key" "github_key" {
  algorithm = "RSA"
}

data "archive_file" "secrets" {
  type        = "zip"
  output_path = "${path.module}/../../secrets_${var.name}.zip"

  source {
    content  = "${data.template_file.aws_credentials.rendered}"
    filename = "awscredentials"
  }

  source {
    content  = "${tls_private_key.github_key.private_key_pem}"
    filename = "id_rsa"
  }

  source {
    content  = "${tls_private_key.github_key.public_key_pem}"
    filename = "id_rsa.pub"
  }
}
