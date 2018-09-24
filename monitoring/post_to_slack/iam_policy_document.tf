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
      "s3:Head*",
      "s3:List*",
    ]

    resources = [
      "${local.snapshots_bucket_arn}/",
      "${local.snapshots_bucket_arn}/*",
    ]
  }
}
