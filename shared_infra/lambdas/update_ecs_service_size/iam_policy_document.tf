data "aws_iam_policy_document" "update_ecs_service_size" {
  statement {
    actions = [
      "ecs:UpdateService",
    ]

    resources = [
      "*",
    ]
  }
}
