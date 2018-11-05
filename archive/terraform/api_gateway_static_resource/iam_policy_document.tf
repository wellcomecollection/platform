data "aws_s3_bucket" "static_content_bucket" {
  bucket = "${var.bucket_name}"
}

data "aws_iam_policy_document" "archive_static_content_get" {
  statement {
    actions = [
      "s3:GetObject*",
    ]

    resources = [
      "${data.aws_s3_bucket.static_content_bucket.arn}/${var.s3_key}",
    ]
  }
}
