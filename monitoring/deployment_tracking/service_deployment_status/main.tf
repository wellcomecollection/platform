# Lambda for tracking deployment status in dynamo db

module "lambda_service_deployment_status" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.0"

  name        = "service_deployment_status"
  description = "Lambda for tracking deployment status in dynamo db"
  timeout     = 10

  environment_variables = {
    TABLE_NAME = "${aws_dynamodb_table.deployments.name}"
  }

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"

  s3_bucket = "${var.infra_bucket}"
  s3_key    = "lambdas/monitoring/deployment_tracking/service_deployment_status.zip"
}

module "trigger_service_deployment_status" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_cloudwatch?ref=v1.0.0"

  lambda_function_name    = "${module.lambda_service_deployment_status.function_name}"
  lambda_function_arn     = "${module.lambda_service_deployment_status.arn}"
  cloudwatch_trigger_arn  = "${var.every_minute_arn}"
  cloudwatch_trigger_name = "${var.every_minute_name}"
}
