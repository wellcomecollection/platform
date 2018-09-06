data "aws_iam_policy_document" "allow_cloudwatch_push_metrics" {
  statement {
    actions = [
      "cloudwatch:PutMetricData",
    ]

    resources = [
      "*",
    ]
  }
}

data "aws_iam_policy_document" "vhs_read_policy" {
  statement {
    actions = [
      "dynamodb:Scan",
    ]

    resources = [
      "${data.aws_dynamodb_table.vhs_table.arn}",
    ]
  }
}
