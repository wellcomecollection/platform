module "window_generator_lambda" {
  source      = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.5"

  name = "sierra_${var.resource_type}_window_generator_${var.resource_type}"

  s3_key      = "lambdas/sierra_adapter/sierra_window_generator.zip"
  module_name = "sierra_window_generator"

  description     = "Generate windows of a specified length and push them to SNS"
  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
  timeout         = 10

  environment_variables = {
    "TOPIC_ARN"             = "${module.windows_topic.arn}"
    "WINDOW_LENGTH_MINUTES" = "${var.window_length_minutes}"
  }
}

module "trigger_sierra_window_generator_lambda" {
  source                  = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_cloudwatch?ref=v1.0.0"
  lambda_function_name    = "${module.window_generator_lambda.function_name}"
  lambda_function_arn     = "${module.window_generator_lambda.arn}"
  cloudwatch_trigger_arn  = "${aws_cloudwatch_event_rule.window_generator_rule.arn}"
  cloudwatch_trigger_name = "${aws_cloudwatch_event_rule.window_generator_rule.id}"
}

module "dynamo_to_sns" {
  source = "git::https://github.com/wellcometrust/platform.git//shared_infra/dynamo_to_sns"

  name           = "sierra_dynamo_to_sns_${var.resource_type}"
  src_stream_arn = "${aws_dynamodb_table.sierra_table.stream_arn}"
  dst_topic_arn  = "${module.sierra_to_dynamo_updates_topic.arn}"

  lambda_error_alarm_arn = "${var.lambda_error_alarm_arn}"
}
