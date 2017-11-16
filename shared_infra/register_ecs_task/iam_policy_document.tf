data "aws_iam_policy_document" "allow_run_task" {
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

data "aws_iam_policy_document" "tasks_table" {
  statement {
    actions = [
      "dynamodb:*",
    ]

    resources = [
      "${aws_dynamodb_table.requested_tasks.arn}",
    ]
  }
}
