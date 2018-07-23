data "aws_sns_topic" "lambda_pushes_topic" {
  name = "${var.lambda_pushes_topic_name}"
}

data "aws_sns_topic" "ecr_pushes_topic" {
  name = "${var.ecr_pushes_topic_name}"
}

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
      "sns:Publish",
    ]

    resources = [
      "${data.aws_sns_topic.lambda_pushes_topic.arn}",
      "${data.aws_sns_topic.ecr_pushes_topic.arn}",
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
}
