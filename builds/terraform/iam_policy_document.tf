data "aws_iam_policy_document" "travis_permissions" {
  statement {
    actions = [
      "s3:*",
    ]

    resources = [
      "${aws_s3_bucket.releases.arn}/*",
      "${aws_s3_bucket.releases.arn}",
    ]
  }
}
