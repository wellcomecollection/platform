# Grafana

resource "aws_iam_role_policy" "ecs_grafana_task_cloudwatch_read" {
  role   = "${module.grafana.role_name}"
  policy = "${var.allow_cloudwatch_read_metrics_policy_json}"
}

# post_to_slack

resource "aws_iam_role_policy" "post_to_slack_get_cloudwatch" {
  role   = "${module.lambda_post_to_slack.role_name}"
  policy = "${var.cloudwatch_allow_filterlogs_policy_json}"
}
