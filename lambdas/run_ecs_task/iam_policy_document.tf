data "aws_iam_policy_document" "allow_cloudwatch_push_metrics" {
  statement {
    actions = [
      "ecs:RunTask",
      "ecs:PassRole",
    ]

    resources = [
      "*",
    ]
  }
}
