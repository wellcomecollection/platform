data "aws_s3_bucket" "static_content_bucket" {
  bucket = "${aws_s3_bucket_object.context.bucket}"
}

data "aws_iam_policy_document" "archive_static_content_get" {
  statement {
    actions = [
      "s3:GetObject*",
    ]

    resources = [
      "${data.aws_s3_bucket.static_content_bucket.arn}/${aws_s3_bucket_object.context.key}",
    ]
  }
}
