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

data "aws_iam_policy_document" "allow_cloudwatch_push_metrics" {
  statement {
    actions = [
      "cloudwatch:PutMetricData",
    ]

    resources = [
      "*",
    ]
  }
}

data "aws_iam_policy_document" "allow_cloudwatch_read_metrics" {
  statement {
    actions = [
      "cloudwatch:DescribeAlarmHistory",
      "cloudwatch:DescribeAlarms",
      "cloudwatch:DescribeAlarmsForMetric",
      "cloudwatch:GetMetricData",
      "cloudwatch:GetMetricStatistics",
      "cloudwatch:ListMetrics",
    ]

    resources = [
      "*",
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

data "aws_iam_policy_document" "miro_images_sync" {
  statement {
    # Easier than listing the _many_ permissions required for s3 sync to work
    actions = [
      "s3:*",
    ]

    resources = [
      "${aws_s3_bucket.miro-images-sync.arn}/*",
      "${aws_s3_bucket.miro-images-sync.arn}",
      "${aws_s3_bucket.miro_images_public.arn}/*",
      "${aws_s3_bucket.miro_images_public.arn}",
    ]
  }

  statement {
    actions = [
      "s3:ListAllMyBuckets",
    ]

    resources = [
      "arn:aws:s3:::*",
    ]
  }
}
