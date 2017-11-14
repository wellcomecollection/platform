data "aws_iam_policy_document" "tasks_table" {
  statement {
    actions = [
      "dynamodb:*",
    ]

    resources = [
      "${aws_dynamodb_table.tasks.arn}",
    ]
  }
}

data "aws_iam_policy_document" "describe_services" {
  statement {
    actions = [
      "ecs:Describe*",
      "ecs:List*",
    ]

    resources = [
      "*",
    ]
  }
}
