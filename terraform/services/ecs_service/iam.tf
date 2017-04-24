resource "aws_iam_role" "ecs_service" {
  name               = "${var.service_name}"
  assume_role_policy = "${data.aws_iam_policy_document.assume_ecs_role.json}"
}

data "aws_iam_policy_document" "assume_ecs_role" {
  statement {
    actions = [
      "sts:AssumeRole",
    ]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role_policy" "ecs_service" {
  name   = "${var.service_name}"
  role   = "${aws_iam_role.ecs_service.name}"
  policy = "${data.aws_iam_policy_document.ecs_service.json}"
}

data "aws_iam_policy_document" "ecs_service" {
  statement {
    actions = [
      "ec2:Describe*",
      "elasticloadbalancing:DeregisterInstancesFromLoadBalancer",
      "elasticloadbalancing:DeregisterTargets",
      "elasticloadbalancing:Describe*",
      "elasticloadbalancing:RegisterInstancesWithLoadBalancer",
      "elasticloadbalancing:RegisterTargets",
    ]

    resources = [
      "*",
    ]
  }
}

resource "aws_iam_role_policy" "read_from_s3" {
  name   = "${var.service_name}_read_from_s3"
  role   = "${aws_iam_role.ecs_service.name}"
  policy = "${data.aws_iam_policy_document.read_from_s3_infra.json}"
}

data "aws_iam_policy_document" "read_from_s3_infra" {
  statement {
    actions = [
      "s3:GetObject",
      "s3:ListBucket",
    ]

    resources = [
      "arn:aws:s3:::${var.infra_bucket}",
    ]
  }
}
