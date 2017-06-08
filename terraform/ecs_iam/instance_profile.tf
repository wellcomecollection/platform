resource "aws_iam_instance_profile" "instance_profile" {
  name = "${var.name}_instance_profile"
  role = "${aws_iam_role.role.name}"
}

data "aws_iam_policy_document" "instance_policy" {
  statement {
    sid = "ecsInstanceRole"

    actions = [
      "ecs:StartTelemetrySession",
      "ecs:DeregisterContainerInstance",
      "ecs:DiscoverPollEndpoint",
      "ecs:Poll",
      "ecs:RegisterContainerInstance",
      "ecs:Submit*",
      "ecr:GetAuthorizationToken",
      "ecr:BatchGetImage",
      "ecr:GetDownloadUrlForLayer",
    ]

    resources = [
      "*",
    ]
  }

  statement {
    sid = "allowLoggingToCloudWatch"

    actions = [
      "logs:CreateLogStream",
      "logs:PutLogEvents",
    ]

    resources = [
      "*",
    ]
  }
}

resource "aws_iam_role_policy" "instance" {
  name   = "${var.name}_instance_role_policy"
  role   = "${aws_iam_role.role.name}"
  policy = "${data.aws_iam_policy_document.instance_policy.json}"
}

data "aws_iam_policy_document" "assume_ec2_role" {
  statement {
    actions = [
      "sts:AssumeRole",
    ]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "role" {
  name               = "${var.name}_instance_role"
  assume_role_policy = "${data.aws_iam_policy_document.assume_ec2_role.json}"
}
