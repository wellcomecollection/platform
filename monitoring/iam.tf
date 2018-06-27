resource "aws_iam_role_policy" "ecs_grafana_task_cloudwatch_read" {
  name = "${local.namespace}_cloudwatch_read"

  role   = "${module.grafana_task.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_read_metrics.json}"
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
