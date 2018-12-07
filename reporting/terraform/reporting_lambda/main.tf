module "reporting_lambda" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v10.2.2"

  name        = "${var.name}"
  description = "${var.description}"
  timeout     = "${var.timeout}"

  environment_variables = "${var.environment_variables}"

  alarm_topic_arn = "${var.error_alarm_topic_arn}"

  s3_bucket = "${data.terraform_remote_state.shared_infra.infra_bucket}"
  s3_key    = "lambdas/reporting/${var.name}.zip"

  log_retention_in_days = "${var.log_retention_in_days}"
}

module "reporting_lambda_trigger" {
  source = "./trigger_sns_subscriptions"

  lambda_function_name = "${module.reporting_lambda.function_name}"
  topic_arns     = "${var.topic_arns}"
}
