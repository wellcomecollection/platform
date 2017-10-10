data "aws_iam_policy_document" "allow_table_capacity_changes" {
  statement {
    actions = [
      "dynamodb:DescribeTable",
      "dynamodb:UpdateTable",
    ]

    resources = [
      "*",
    ]
  }
}
