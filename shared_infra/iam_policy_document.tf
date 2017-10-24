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
    ]

    resources = [
      "${aws_s3_bucket.infra.arn}/lambdas/*",
      "${aws_s3_bucket.infra.arn}/releases/*",
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
}

data "aws_iam_policy_document" "alb_logs" {
  statement {
    actions = [
      "s3:PutObject",
    ]

    resources = [
      "arn:aws:s3:::wellcomecollection-alb-logs/*",
    ]

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
