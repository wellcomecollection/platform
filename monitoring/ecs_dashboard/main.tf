module "update_service_list" {
  source = "update_service_list"

  every_minute_name = "${var.every_minute_name}"
  every_minute_arn  = "${var.every_minute_arn}"

  dashboard_bucket          = "${var.dashboard_bucket}"
  dashboard_assumable_roles = "${var.dashboard_assumable_roles}"

  lambda_error_alarm_arn = "${var.lambda_error_alarm_arn}"
  infra_bucket           = "${var.infra_bucket}"
}
