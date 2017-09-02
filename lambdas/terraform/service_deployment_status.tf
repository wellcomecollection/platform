# Lambda for tracking deployment status in dynamo db

module "lambda_service_deployment_status" {
  source      = "./lambda"
  name        = "service_deployment_status"
  description = "Lambda for tracking deployment status in dynamo db"
  source_dir  = "../src/service_deployment_status"
  timeout     = 10

  environment_variables = {
    TABLE_NAME = "${aws_dynamodb_table.deployments.name}"
  }

  alarm_topic_arn = "${data.terraform_remote_state.platform.lambda_error_alarm_arn}"
}

module "trigger_service_deployment_status" {
  source                  = "./lambda/trigger_cloudwatch"
  lambda_function_name    = "${module.lambda_service_deployment_status.function_name}"
  lambda_function_arn     = "${module.lambda_service_deployment_status.arn}"
  cloudwatch_trigger_arn  = "${aws_cloudwatch_event_rule.every_minute.arn}"
  cloudwatch_trigger_name = "${aws_cloudwatch_event_rule.every_minute.name}"
}
