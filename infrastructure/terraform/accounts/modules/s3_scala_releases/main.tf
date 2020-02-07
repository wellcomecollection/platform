data "aws_iam_policy_document" "assume_role_policy" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      identifiers = [local.aws_principal]
      type        = "AWS"
    }
  }
}

resource "aws_iam_role" "s3_scala_releases" {
  name               = "${var.name}-s3_scala_releases"
  assume_role_policy = data.aws_iam_policy_document.assume_role_policy.json
}

resource "aws_iam_role_policy" "s3_scala_releases" {
  policy = data.aws_iam_policy_document.s3_scala_releases.json
  role   = aws_iam_role.s3_scala_releases.name
}

data "aws_iam_policy_document" "s3_scala_releases" {
  statement {
    actions = [
      "s3:*",
    ]

    resources = [
      "${var.bucket_arn}/uk/ac/wellcome/${var.name}_2.12/*",
      "${var.bucket_arn}/uk/ac/wellcome/${var.name}_typesafe_2.12/*",
    ]
  }
}

data "aws_caller_identity" "current" {}

locals {
  account_id    = data.aws_caller_identity.current.account_id
  aws_principal = "arn:aws:iam::${local.account_id}:root"
}
