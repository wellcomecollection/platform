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
