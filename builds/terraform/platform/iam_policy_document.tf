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
      "s3:Get*",
      "s3:List*",
    ]

    resources = [
      "${var.sbt_releases_bucket_arn}",
      "${var.sbt_releases_bucket_arn}/*",
    ]
  }

  statement {
    actions = [
      "s3:*",
    ]

    resources = [
      "${var.infra_bucket_arn}/lambdas/*",
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
      "sns:Publish",
    ]

    resources = [
      "arn:aws:sns:eu-west-1:760097843905:lambda_pushes",
    ]
  }

  statement {
    actions = [
      "ssm:PutParameter",
    ]

    resources = [
      "arn:aws:ssm:eu-west-1:${local.account_id}:parameter/*",
    ]
  }
}

locals {
  account_id = "${data.aws_caller_identity.current.account_id}"
}

data "aws_caller_identity" "current" {}
