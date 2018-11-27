# Grafana

resource "aws_iam_role_policy" "ecs_grafana_task_cloudwatch_read" {
  name = "${local.namespace}_cloudwatch_read"

  role   = "${module.grafana_task.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_read_metrics.json}"
}

# ECS Dashboard

resource "aws_iam_role_policy" "update_service_list_describe_services" {
  role   = "${module.lambda_update_service_list.role_name}"
  policy = "${data.aws_iam_policy_document.describe_services.json}"
}

resource "aws_iam_role_policy" "update_service_list_push_to_s3" {
  role   = "${module.lambda_update_service_list.role_name}"
  policy = "${data.aws_iam_policy_document.s3_put_dashboard_status.json}"
}

resource "aws_iam_role_policy" "update_service_list_read_from_webplatform" {
  role   = "${module.lambda_update_service_list.role_name}"
  policy = "${data.aws_iam_policy_document.assume_roles.json}"
}

# post_to_slack

resource "aws_iam_role_policy" "post_to_slack_get_cloudwatch" {
  role   = "${module.lambda_post_to_slack.role_name}"
  policy = "${data.aws_iam_policy_document.cloudwatch_allow_filterlogs.json}"
}

# slack_budget_bot

resource "aws_iam_role_policy" "allow_s3_write" {
  role   = "${module.ecs_slack_budget_bot_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_s3_write.json}"
}

resource "aws_iam_role_policy" "allow_describe_budgets" {
  role   = "${module.ecs_slack_budget_bot_iam.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_describe_budgets.json}"
}
