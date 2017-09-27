# Lambda for tracking deployment status in dynamo db

module "lambda_service_deployment_status" {
  source = "../../terraform/lambda"

  name        = "service_deployment_status"
  description = "Lambda for tracking deployment status in dynamo db"
  timeout     = 10

  environment_variables = {
    TABLE_NAME = "${var.dynamodb_table_deployments_name}"
  }

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
  s3_key          = "lambdas/lambdas/service_deployment_status.zip"
}

module "trigger_service_deployment_status" {
  source = "../../terraform/lambda/trigger_cloudwatch"

  lambda_function_name    = "${module.lambda_service_deployment_status.function_name}"
  lambda_function_arn     = "${module.lambda_service_deployment_status.arn}"
  cloudwatch_trigger_arn  = "${var.every_minute_arn}"
  cloudwatch_trigger_name = "${var.every_minute_name}"
}
