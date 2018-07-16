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

resource "local_file" "aws_credentials" {
  filename = "${path.module}/secrets_${var.name}/aws_credentials"
  content  = "${data.template_file.aws_credentials.rendered}"
}
