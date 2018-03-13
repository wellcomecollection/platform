data "aws_s3_bucket" "uploads" {
  bucket = "${var.upload_bucket}"
}

data "aws_iam_policy_document" "allow_s3_write" {
  statement {
    actions = [
      "s3:PutObject",
    ]

    resources = [
      "${data.aws_s3_bucket.uploads.arn}/elasticdump/*",
    ]
  }
}
