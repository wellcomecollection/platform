module "lambda_update_service_list" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v10.2.2"

  s3_bucket = "${var.infra_bucket}"
  s3_key    = "lambdas/monitoring/update_service_list.zip"

  name        = "update_service_list"
  description = "Publish ECS service status summary to S3"

  # We've seen timeouts at 60 seconds on these Lambdas, so set the max
  # timeout to make a timeout as unlikely as possible.
  timeout = 300

  environment_variables = {
    BUCKET_NAME     = "${var.dashboard_bucket}"
    OBJECT_KEY      = "data/ecs_status.json"
    ASSUMABLE_ROLES = "${join(",", var.dashboard_assumable_roles)}"
  }

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"

  log_retention_in_days = 14
}

module "trigger_update_service_list" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_cloudwatch?ref=v1.0.0"

  lambda_function_name    = "${module.lambda_update_service_list.function_name}"
  lambda_function_arn     = "${module.lambda_update_service_list.arn}"
  cloudwatch_trigger_arn  = "${var.every_minute_rule_arn}"
  cloudwatch_trigger_name = "${var.every_minute_rule_name}"
}
