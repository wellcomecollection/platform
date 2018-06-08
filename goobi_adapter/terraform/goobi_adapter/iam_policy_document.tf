data "aws_s3_bucket" "goobi_data" {
  bucket = "${var.goobi_mets_bucket_name}"
}

data "aws_iam_policy_document" "allow_s3_access" {
  statement {
    actions = [
      "s3:GetObject",
    ]

    resources = [
      "${data.aws_s3_bucket.goobi_data.arn}",
      "${data.aws_s3_bucket.goobi_data.arn}/*",
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
