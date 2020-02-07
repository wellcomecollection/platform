# Intranda

# Users

resource "aws_iam_user" "intranda_editorial_photography" {
  name = "intranda_editorial_photography"
}

resource "aws_iam_user_policy" "wellcomecollection_editorial_photography_read_write" {
  user   = aws_iam_user.intranda_editorial_photography.name
  policy = data.aws_iam_policy_document.s3_wellcomecollection_editorial_photography_bucket_read_write.json
}

resource "aws_iam_user_policy" "editorial_photography_export_read_write" {
  user   = aws_iam_user.intranda_editorial_photography.name
  policy = data.aws_iam_policy_document.allow_external_export-bagit_access.json
}

# Policy documents

data "aws_iam_policy_document" "s3_wellcomecollection_editorial_photography_bucket_read_write" {
  statement {
    actions = [
      "s3:*",
    ]

    resources = [
      "${aws_s3_bucket.editorial_photography.arn}/*",
      "${aws_s3_bucket.editorial_photography.arn}",
    ]
  }

  statement {
    effect = "Deny"

    actions = [
      "s3:DeleteBucket*",
      "s3:PutBucket*",
      "s3:PutEncryptionConfiguration",
      "s3:PutInventoryConfiguration",
      "s3:PutLifecycleConfiguration",
      "s3:PutMetricsConfiguration",
      "s3:PutReplicationConfiguration",
    ]

    resources = [
      "${aws_s3_bucket.editorial_photography.arn}/*",
      "${aws_s3_bucket.editorial_photography.arn}",
    ]
  }
}
