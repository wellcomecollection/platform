data "aws_iam_policy_document" "travis_permissions" {
  statement {
    actions = [
      "ecr:*",
    ]

    resources = [
      "*",
    ]
  }

  statement {
    actions = [
      "s3:PutObject",
      "s3:GetObject",
      "s3:HeadObject",

      # Required in case Travis tries to publish a Lambda that doesn't exist
      # yet.  It complains about not having HeadObject permissions, but it
      # really needs both of them.
      "s3:ListObject",
    ]

    resources = [
      "${var.infra_bucket_arn}/lambdas/*",
      "${var.infra_bucket_arn}/releases/*",
    ]
  }

  statement {
    actions = [
      "sns:ListTopic",
    ]

    resources = [
      "*",
    ]
  }

  statement {
    actions = [
      "iam:GetUser",
    ]

    resources = [
      "*",
    ]
  }

  statement {
    actions = [
      "s3:Get*",
      "s3:List*",
    ]

    resources = [
      "${var.sbt_releases_bucket_arn}/*",
    ]
  }

  statement {
    actions = [
      "ssm:PutParameter",
    ]

    resources = [
      "arn:aws:ssm:eu-west-1:${local.account_id}:parameter/releases/*",
    ]
  }
}

locals {
  account_id = "${data.aws_caller_identity.current.account_id}"
}

data "aws_caller_identity" "current" {}
