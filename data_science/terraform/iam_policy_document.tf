data "aws_iam_policy_document" "data_science_bucket" {
  statement {
    actions = [
      "s3:*",
    ]

    resources = [
      "${aws_s3_bucket.jupyter.arn}",
      "${aws_s3_bucket.jupyter.arn}/*",
    ]
  }
}

data "aws_iam_policy_document" "data_science_readall" {
  statement {
    actions = [
      "s3:Get*",
    ]

    resources = [
      "*",
    ]
  }
}
