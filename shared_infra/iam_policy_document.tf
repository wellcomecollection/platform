data "aws_iam_policy_document" "deployments_table" {
  statement {
    actions = [
      "dynamodb:*",
    ]

    resources = [
      "${aws_dynamodb_table.deployments.arn}",
    ]
  }
}

data "aws_iam_policy_document" "describe_services" {
  statement {
    actions = [
      "ecs:DescribeServices",
      "ecs:DescribeClusters",
      "ecs:DescribeTaskDefinition",
      "ecs:ListClusters",
      "ecs:ListServices",
    ]

    resources = [
      "*",
    ]
  }
}

data "aws_sns_topic" "lambda_pushes_topic" {
  name = "${local.lambda_pushes_topic_name}"
}

data "aws_sns_topic" "ecr_pushes_topic" {
  name = "${local.ecr_pushes_topic_name}"
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

      # Required in case Travis tries to publish a Lambda that doesn't exist
      # yet.  It complains about not having HeadObject permissions, but it
      # really needs both of them.
      "s3:HeadObject",
      "s3:ListObject",
    ]

    resources = [
      "${aws_s3_bucket.platform_infra.arn}/lambdas/*",
      "${aws_s3_bucket.platform_infra.arn}/releases/*",
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
}

data "aws_iam_policy_document" "s3_alb_logs" {
  statement {
    actions = [
      "s3:PutObject",
    ]

    resources = [
      "arn:aws:s3:::${local.alb_logs_bucket_name}/*",
    ]

    # This is the Account ID for Elastic Load Balancing; not another
    # AWS account at Wellcome.
    # See https://docs.aws.amazon.com/elasticloadbalancing/latest/classic/enable-access-logs.html
    principals {
      identifiers = ["arn:aws:iam::156460612806:root"]
      type        = "AWS"
    }
  }
}

data "aws_iam_policy_document" "sqs_readwrite" {
  statement {
    actions = [
      "sqs:DeleteMessage",
      "sqs:ReceiveMessage",
    ]

    resources = [
      "*",
    ]
  }
}

data "aws_iam_policy_document" "sqs_readwritesend" {
  statement {
    actions = [
      "sqs:DeleteMessage",
      "sqs:ReceiveMessage",
      "sqs:SendMessage",
    ]

    resources = [
      "*",
    ]
  }
}

data "aws_iam_policy_document" "upload_sqs_freeze" {
  statement {
    actions = [
      "s3:PutObject",
    ]

    resources = [
      "${aws_s3_bucket.platform_infra.arn}/sqs/*",
    ]
  }
}
