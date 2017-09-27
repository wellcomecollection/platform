# Lambda for publishing out of date deployments to SNS

module "lambda_notify_old_deploys" {
  source = "../../terraform/lambda"

  name        = "notify_old_deploys"
  description = "For publishing out of date deployments to SNS"

  environment_variables = {
    TABLE_NAME        = "${var.dynamodb_table_deployments_name}"
    TOPIC_ARN         = "${var.old_deployments_arn}"
    AGE_BOUNDARY_MINS = "5"
  }

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
  s3_key          = "lambdas/lambdas/notify_old_deploys.zip"
}

module "trigger_notify_old_deploys" {
  source = "../../terraform/lambda/trigger_cloudwatch"

  lambda_function_name    = "${module.lambda_notify_old_deploys.function_name}"
  lambda_function_arn     = "${module.lambda_notify_old_deploys.arn}"
  cloudwatch_trigger_arn  = "${var.every_minute_arn}"
  cloudwatch_trigger_name = "${var.every_minute_name}"
}
