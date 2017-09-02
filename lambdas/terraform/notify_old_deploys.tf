# Lambda for publishing out of date deployments to SNS

module "lambda_notify_old_deploys" {
  source      = "./lambda"
  name        = "notify_old_deploys"
  description = "For publishing out of date deployments to SNS"
  source_dir  = "../src/notify_old_deploys"

  environment_variables = {
    TABLE_NAME        = "${data.terraform_remote_state.platform.dynamodb_table_deployments_name}"
    TOPIC_ARN         = "${data.terraform_remote_state.platform.old_deployments_arn}"
    AGE_BOUNDARY_MINS = "5"
  }

  alarm_topic_arn = "${data.terraform_remote_state.platform.lambda_error_alarm_arnn}"
}

module "trigger_notify_old_deploys" {
  source                  = "./lambda/trigger_cloudwatch"
  lambda_function_name    = "${module.lambda_notify_old_deploys.function_name}"
  lambda_function_arn     = "${module.lambda_notify_old_deploys.arn}"
  cloudwatch_trigger_arn  = "${aws_cloudwatch_event_rule.every_minute.arn}"
  cloudwatch_trigger_name = "${aws_cloudwatch_event_rule.every_minute.name}"
}
