# Grafana

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

# ECS Dashboard

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

data "aws_iam_policy_document" "assume_roles" {
  statement {
    actions = ["sts:AssumeRole"]

    resources = [
      "arn:aws:iam::130871440101:role/platform-team-assume-role",
      "arn:aws:iam::299497370133:role/monitoring",
      "arn:aws:iam::975596993436:role/monitoring",
      "arn:aws:iam::760097843905:role/monitoring"
    ]
  }
}

# post_to_slack

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

# slack_budget_bot

data "aws_iam_policy_document" "allow_s3_write" {
  statement {
    actions = [
      "s3:Put*",
    ]

    resources = [
      "${aws_s3_bucket.monitoring.arn}/budget_graphs/*",
    ]
  }
}

data "aws_iam_policy_document" "allow_describe_budgets" {
  statement {
    actions = [
      "budgets:ViewBudget",
    ]

    resources = [
      "*",
    ]
  }
}
