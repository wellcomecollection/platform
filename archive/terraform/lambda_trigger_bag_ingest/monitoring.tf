module "lambda_trigger_bag_ingest_monitoring" {
  source          = "git::https://github.com/wellcometrust/terraform.git//lambda/modules/monitoring?ref=v17.1.0"
  name            = "${var.name}"
  alarm_topic_arn = "${var.lambda_error_alarm_arn}"

  iam_role_name         = "${module.lambda_trigger_bag_ingest_iam.role_name}"
  log_retention_in_days = "30"
}
