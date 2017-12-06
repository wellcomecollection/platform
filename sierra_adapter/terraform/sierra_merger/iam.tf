data "aws_iam_policy_document" "sierra_table_permissions" {
  statement {
    actions = [
      "dynamodb:*",
    ]

    resources = [
      "${var.target_dynamo_table_arn}",
    ]
  }
}

resource "aws_iam_role_policy" "allow_dynamo_access" {
  role   = "${module.sierra_merger_service.task_role_name}"
  policy = "${data.aws_iam_policy_document.sierra_table_permissions.json}"
}
