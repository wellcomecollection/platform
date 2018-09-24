data "aws_iam_policy_document" "cloudwatch_allow_filterlogs" {
  statement {
    actions = [
      "logs:FilterLogEvents",
    ]

    resources = [
      "*",
    ]
  }

  statement {
    actions = [
      "s3:ListObjects",
    ]

    resources = [
      "${local.snapshots_bucket_arn}/*"
    ]
  }
}
