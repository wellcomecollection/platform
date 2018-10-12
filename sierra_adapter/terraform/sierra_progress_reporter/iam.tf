data "aws_iam_policy_document" "allow_s3_access" {
  statement {
    actions = [
      "s3:*",
    ]

    resources = [
      "arn:aws:s3:::${var.s3_adapter_bucket_name}",
      "arn:aws:s3:::${var.s3_adapter_bucket_name}/*",
    ]
  }
}

resource "aws_iam_role_policy" "allow_s3_access" {
  role   = "${module.lambda.role_name}"
  policy = "${data.aws_iam_policy_document.allow_s3_access.json}"
}
