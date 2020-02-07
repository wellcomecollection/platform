data "aws_iam_policy_document" "s3_backups_full_access" {
  statement {
    actions = [
      "s3:*",
    ]

    resources = [
      aws_s3_bucket.photography_backups.arn,
      "${aws_s3_bucket.photography_backups.arn}/*",
    ]
  }

  statement {
    actions = [
      "s3:List*",
    ]

    resources = [
      "*",
    ]
  }
}
