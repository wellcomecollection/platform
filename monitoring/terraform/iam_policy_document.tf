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

data "aws_iam_policy_document" "s3_put_dashboard_status" {
  statement {
    actions = [
      "s3:PutObject",
      "s3:GetObjectACL",
      "s3:PutObjectACL",
    ]

    resources = [
      "${aws_s3_bucket.dashboard.arn}/data/*",
    ]
  }
}

data "aws_iam_policy_document" "s3_put_gatling_reports" {
  statement {
    actions = [
      "s3:PutObject",
      "s3:GetObjectACL",
      "s3:PutObjectACL",
    ]

    resources = [
      "${aws_s3_bucket.dashboard.arn}/gatling/*",
    ]
  }
}

data "aws_iam_policy_document" "cloudwatch_allow_filterlogs" {
  statement {
    actions = [
      "logs:FilterLogEvents",
    ]

    resources = [
      "*",
    ]
  }
}
