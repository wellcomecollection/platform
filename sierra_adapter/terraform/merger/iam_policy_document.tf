data "aws_dynamodb_table" "merged_sierra_records" {
  name = "${var.merged_dynamo_table_name}"
}

data "aws_iam_policy_document" "sierra_merged_table_permissions" {
  statement {
    actions = [
      "dynamodb:*",
    ]

    resources = [
      "${data.aws_dynamodb_table.merged_sierra_records.arn}",
    ]
  }
}
