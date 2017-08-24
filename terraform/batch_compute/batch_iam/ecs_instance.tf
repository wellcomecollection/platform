data "aws_iam_policy_document" "ecs_instance" {
  statement {
    actions = [
      "ecs:CreateCluster",
      "ecs:DeregisterContainerInstance",
      "ecs:DiscoverPollEndpoint",
      "ecs:Poll",
      "ecs:RegisterContainerInstance",
      "ecs:StartTelemetrySession",
      "ecs:UpdateContainerInstancesState",
      "ecs:Submit*",
      "ecr:GetAuthorizationToken",
      "ecr:BatchCheckLayerAvailability",
      "ecr:GetDownloadUrlForLayer",
      "ecr:BatchGetImage",
      "logs:CreateLogStream",
      "logs:PutLogEvents",
    ]

    resources = [
      "*",
    ]
  }
}

data "aws_iam_policy_document" "ecs_instance_assume_role" {
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

resource "aws_iam_role_policy" "ecs_instance" {
  name   = "${var.prefix}_EcsInstanceRolePolicy"
  role   = "${aws_iam_role.ecs_instance.name}"
  policy = "${data.aws_iam_policy_document.ecs_instance.json}"
}

resource "aws_iam_role" "ecs_instance" {
  name               = "${var.prefix}_EcsInstanceRole"
  assume_role_policy = "${data.aws_iam_policy_document.ecs_instance_assume_role.json}"
}

resource "aws_iam_instance_profile" "ecs_instance" {
  name = "${var.prefix}_EcsInstanceRole"
  role = "${aws_iam_role.ecs_instance.name}"
}
