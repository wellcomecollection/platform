data "aws_iam_policy_document" "data_science_bucket" {
  statement {
    actions = [
      "s3:*",
    ]

    resources = [
      "${aws_s3_bucket.data_science.arn}",
      "${aws_s3_bucket.data_science.arn}/*",
    ]
  }
}

data "aws_iam_policy_document" "data_science_readall" {
  statement {
    actions = [
      "s3:Get*",
    ]

    resources = [
      "*"
    ]
  }
}