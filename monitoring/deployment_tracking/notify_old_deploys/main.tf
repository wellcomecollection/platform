# Lambda for publishing out of date deployments to SNS

module "lambda_notify_old_deploys" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.0"

  name        = "notify_old_deploys"
  description = "For publishing out of date deployments to SNS"

  environment_variables = {
    TABLE_NAME        = "${var.dynamodb_table_deployments_name}"
    TOPIC_ARN         = "${module.old_deployments.arn}"
    AGE_BOUNDARY_MINS = "5"
  }

  timeout = 10

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"

  s3_bucket = "${var.infra_bucket}"
  s3_key    = "lambdas/monitoring/deployment_tracking/notify_old_deploys.zip"
}

module "trigger_notify_old_deploys" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_cloudwatch?ref=v1.0.0"

  lambda_function_name    = "${module.lambda_notify_old_deploys.function_name}"
  lambda_function_arn     = "${module.lambda_notify_old_deploys.arn}"
  cloudwatch_trigger_arn  = "${var.every_minute_arn}"
  cloudwatch_trigger_name = "${var.every_minute_name}"
}
