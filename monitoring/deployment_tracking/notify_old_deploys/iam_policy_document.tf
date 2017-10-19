data "aws_iam_policy_document" "deployments_table" {
  statement {
    actions = [
      "dynamodb:*",
    ]

    resources = [
      "${var.dynamodb_table_deployments_arn}",
    ]
  }
}
