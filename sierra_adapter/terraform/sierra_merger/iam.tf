data "aws_dynamodb_table" "target" {
  name = "${var.target_dynamo_table_name}"
}

data "aws_iam_policy_document" "sierra_table_permissions" {
  statement {
    actions = [
      "dynamodb:*",
    ]

    resources = [
      "${data.aws_dynamodb_table.target.arn}",
    ]
  }
}

resource "aws_iam_role_policy" "allow_dynamo_access" {
  role   = "${module.sierra_merger_service.task_role_name}"
  policy = "${data.aws_iam_policy_document.sierra_table_permissions.json}"
}
