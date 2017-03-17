resource "aws_iam_instance_profile" "instance_profile" {
  name  = "${var.name}_instance_profile"
  roles = ["${aws_iam_role.role.name}"]
}

data "template_file" "instance_policy" {
  template = "${file("${path.module}/instance-policy.json.template")}"
}

resource "aws_iam_role_policy" "instance" {
  name   = "${var.name}_instance_role_policy"
  role   = "${aws_iam_role.role.name}"
  policy = "${data.template_file.instance_policy.rendered}"
}

resource "aws_iam_role" "role" {
  name = "${var.name}_instance_role"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "",
      "Effect": "Allow",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF
}
