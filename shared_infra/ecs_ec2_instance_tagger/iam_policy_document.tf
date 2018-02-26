data "aws_s3_bucket" "infra" {
  bucket = "${var.infra_bucket}"
}

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
      "${data.aws_s3_bucket.infra.arn}/tmp/*",
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
