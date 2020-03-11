# Grafana

resource "aws_iam_role_policy" "ecs_grafana_task_cloudwatch_read" {
  role   = "${module.grafana.role_name}"
  policy = "${var.allow_cloudwatch_read_metrics_policy_json}"
}

# ECS Dashboard

resource "aws_iam_role_policy" "update_service_list_describe_services" {
  role   = "${module.lambda_update_service_list.role_name}"
  policy = "${var.describe_services_policy_json}"
}

variable "s3_put_dashboard_status_policy_json" {}

resource "aws_iam_role_policy" "update_service_list_push_to_s3" {
  role   = "${module.lambda_update_service_list.role_name}"
  policy = "${var.s3_put_dashboard_status_policy_json}"
}

resource "aws_iam_role_policy" "update_service_list_read_from_webplatform" {
  role   = "${module.lambda_update_service_list.role_name}"
  policy = "${var.assume_roles_policy_json}"
}

# post_to_slack

resource "aws_iam_role_policy" "post_to_slack_get_cloudwatch" {
  role   = "${module.lambda_post_to_slack.role_name}"
  policy = "${var.cloudwatch_allow_filterlogs_policy_json}"
}
