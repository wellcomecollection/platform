data "aws_iam_policy_document" "write_ec2_tags" {
  statement {
    actions = [
      "ec2:createTags",
    ]

    resources = [
      "*",
    ]
  }
}

data "aws_iam_policy_document" "s3_put_infra_tmp" {
  statement {
    actions = [
      "s3:*",
    ]

    resources = [
      "${var.bucket_infra_arn}/tmp/*",
    ]
  }

  statement {
    actions = [
      "s3:ListBucket",
    ]

    resources = [
      "arn:aws:s3:::*",
    ]
  }
}
