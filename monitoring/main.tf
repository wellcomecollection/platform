module "grafana" {
  source = "grafana"

  ecs_monitoring_iam_instance_role_name = "${module.ecs_monitoring_iam.instance_role_name}"

  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"

  listener_https_arn = "${module.monitoring_alb.listener_https_arn}"
  listener_http_arn  = "${module.monitoring_alb.listener_http_arn}"

  vpc_id              = "${module.vpc_monitoring.vpc_id}"
  cluster_id          = "${aws_ecs_cluster.monitoring.id}"
  efs_mount_directory = "${module.monitoring_userdata.efs_mount_directory}"

  cloudwatch_id = "${module.monitoring_alb.cloudwatch_id}"

  release_ids = "${var.release_ids}"

  grafana_admin_user        = "${var.grafana_admin_user}"
  grafana_anonymous_role    = "${var.grafana_anonymous_role}"
  grafana_admin_password    = "${var.grafana_admin_password}"
  grafana_anonymous_enabled = "${var.grafana_anonymous_enabled}"
}

module "deployment_tracking" {
  source = "deployment_tracking"

  every_minute_arn  = "${aws_cloudwatch_event_rule.every_minute.arn}"
  every_minute_name = "${aws_cloudwatch_event_rule.every_minute.name}"

  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
}

module "ecs_dashboard" {
  source = "ecs_dashboard"

  dashboard_assumable_roles = "${var.dashboard_assumable_roles}"
  dash_bucket               = "${var.dash_bucket}"

  every_minute_arn  = "${aws_cloudwatch_event_rule.every_minute.arn}"
  every_minute_name = "${aws_cloudwatch_event_rule.every_minute.name}"

  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
}

module "load_test" {
  source = "load_test"

  bucket_dashboard_id  = "${module.ecs_dashboard.bucket_dashboard_id}"
  bucket_dashboard_arn = "${module.ecs_dashboard.bucket_dashboard_arn}"

  release_ids             = "${var.release_ids}"
  every_5_minutes_name    = "${aws_cloudwatch_event_rule.every_5_minutes.name}"
  aws_region              = "${var.aws_region}"
  ecs_services_cluster_id = "${local.ecs_services_cluster_id}"

  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
}

module "post_to_slack" {
  source = "post_to_slack"

  lambda_error_alarm_arn                          = "${local.lambda_error_alarm_arn}"
  terminal_failure_alarm_arn                      = "${local.terminal_failure_alarm_arn}"
  alb_server_error_alarm_arn                      = "${local.alb_server_error_alarm_arn}"
  critical_slack_webhook                          = "${var.critical_slack_webhook}"
  non_critical_slack_webhook                      = "${var.non_critical_slack_webhook}"
  dlq_alarm_arn                                   = "${local.dlq_alarm_arn}"
  bitly_access_token                              = "${var.bitly_access_token}"
  ec2_instance_terminating_for_too_long_alarm_arn = "${local.ec2_instance_terminating_for_too_long_alarm_arn}"
}

module "slack_budget_bot" {
  source = "slack_budget_bot"

  release_ids = "${var.release_ids}"
  dashboard_bucket_id = "${var.dash_bucket}"
  account_id = "${data.aws_caller_identity.current.account_id}"
}

module "terraform_tracker" {
  source                     = "terraform_tracker"
  lambda_error_alarm_arn     = "${local.lambda_error_alarm_arn}"
  terraform_apply_topic_name = "${local.terraform_apply_topic_name}"
  slack_webhook              = "${var.non_critical_slack_webhook}"
  bitly_access_token         = "${var.bitly_access_token}"
}
