module "update_service_list" {
  source = "update_service_list"

  every_minute_name = "${var.every_minute_name}"
  every_minute_arn  = "${var.every_minute_arn}"

  bucket_dashboard_id       = "${var.dashboard_bucket_id}"
  dashboard_assumable_roles = "${var.dashboard_assumable_roles}"

  lambda_error_alarm_arn = "${var.lambda_error_alarm_arn}"
}
