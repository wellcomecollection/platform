module "service_deployment_status" {
  source = "service_deployment_status"

  every_minute_name = "${var.every_minute_name}"
  every_minute_arn  = "${var.every_minute_arn}"

  lambda_error_alarm_arn = "${var.lambda_error_alarm_arn}"
}

module "notify_old_deploys" {
  source = "notify_old_deploys"

  every_minute_name = "${var.every_minute_name}"
  every_minute_arn  = "${var.every_minute_arn}"

  dynamodb_table_deployments_name = "${module.service_deployment_status.dynamodb_table_deployments_name}"
  dynamodb_table_deployments_arn  = "${module.service_deployment_status.dynamodb_table_deployments_arn}"

  lambda_error_alarm_arn = "${var.lambda_error_alarm_arn}"
}

module "task_tracking" {
  source = "task_tracking"

  cluster_name = "${var.task_tracking_cluster_name}"

  every_minute_name = "${var.every_minute_name}"
  every_minute_arn  = "${var.every_minute_arn}"

  lambda_error_alarm_arn = "${var.lambda_error_alarm_arn}"
}

module "task_status_notifier" {
  source = "task_status_notifier"

  every_minute_name = "${var.every_minute_name}"
  every_minute_arn  = "${var.every_minute_arn}"

  sns_trigger_arn = "${module.task_tracking.task_updates_topic_arn}"

  lambda_error_alarm_arn = "${var.lambda_error_alarm_arn}"
}
