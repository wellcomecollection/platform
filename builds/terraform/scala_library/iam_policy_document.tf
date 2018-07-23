data "aws_iam_policy_document" "travis_permissions" {
  statement {
    actions = [
      "s3:*",
    ]

    resources = [
      "${var.bucket_arn}/uk/ac/wellcome/${var.name}_2.12/*",
    ]
  }

  statement {
    actions = [
      "s3:ListBucket",
    ]

    resources = [
      "${var.bucket_arn}",
    ]
  }
}
