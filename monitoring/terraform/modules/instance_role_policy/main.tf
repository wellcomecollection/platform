data "aws_iam_policy_document" "instance_policy" {
  statement {
    actions = [
      "ecr:BatchGetImage",
      "ecr:GetAuthorizationToken",
      "ecr:GetDownloadUrlForLayer",
      "ecs:DeregisterContainerInstance",
      "ecs:DiscoverPollEndpoint",
      "ecs:Poll",
      "ecs:RegisterContainerInstance",
      "ecs:StartTelemetrySession",
      "ecs:Submit*",
    ]

    resources = [
      "*",
    ]
  }
}

resource "aws_iam_role_policy" "instance" {
  name = "${var.cluster_name}_instance_role_policy_ecs"

  role   = "${var.instance_profile_role_name}"
  policy = "${data.aws_iam_policy_document.instance_policy.json}"
}
