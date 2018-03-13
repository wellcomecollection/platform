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


data "aws_iam_policy_document" "private_data_bucket_full_access_policy" {
  statement {
    actions = [
      "s3:*",
    ]

    resources = [
      "${aws_s3_bucket.private_data.arn}/",
      "${aws_s3_bucket.private_data.arn}/*",
    ]
  }
}

data "aws_iam_policy_document" "public_data_bucket_full_access_policy" {
  statement {
    actions = [
      "s3:*",
    ]

    resources = [
      "${aws_s3_bucket.public_data.arn}/",
      "${aws_s3_bucket.public_data.arn}/*",
    ]
  }
}