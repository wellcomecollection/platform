module "service_deployment_status" {
  source = "service_deployment_status"

  every_minute_name = "${var.every_minute_name}"
  every_minute_arn  = "${var.every_minute_arn}"

  lambda_error_alarm_arn = "${var.lambda_error_alarm_arn}"
  infra_bucket           = "${var.infra_bucket}"
}

module "notify_old_deploys" {
  source = "notify_old_deploys"

  every_minute_name = "${var.every_minute_name}"
  every_minute_arn  = "${var.every_minute_arn}"

  dynamodb_table_deployments_name = "${module.service_deployment_status.dynamodb_table_deployments_name}"
  dynamodb_table_deployments_arn  = "${module.service_deployment_status.dynamodb_table_deployments_arn}"

  lambda_error_alarm_arn = "${var.lambda_error_alarm_arn}"
  infra_bucket           = "${var.infra_bucket}"
}
