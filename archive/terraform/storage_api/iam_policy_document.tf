data "aws_iam_policy_document" "archive_static_content_get" {
  statement {
    actions = [
      "s3:GetObject*",
    ]

    resources = [
      "${aws_s3_bucket_object.context.bucket}/${aws_s3_bucket_object.context.key}",
    ]
  }
}
