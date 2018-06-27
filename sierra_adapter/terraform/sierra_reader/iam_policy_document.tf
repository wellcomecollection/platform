data "aws_s3_bucket" "sierra_data" {
  bucket = "${var.bucket_name}"
}

data "aws_iam_policy_document" "allow_s3_access" {
  statement {
    actions = [
      "s3:*",
    ]

    resources = [
      "${data.aws_s3_bucket.sierra_data.arn}",
      "${data.aws_s3_bucket.sierra_data.arn}/*",
    ]
  }
}

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
