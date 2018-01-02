module "lambda_sierra_window_generator" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.5"

  description = "Generate update windows for ${var.resource_type} and push them to SNS"
  name        = "sierra_${var.resource_type}_window_generator"

  s3_key      = "lambdas/sierra_adapter/sierra_window_generator.zip"
  module_name = "sierra_window_generator"

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"

  environment_variables = {
    "TOPIC_ARN"             = "${module.topic_sierra_windows.arn}"
    "WINDOW_LENGTH_MINUTES" = "${var.window_length_minutes}"
  }
}

module "trigger_sierra_window_generator_lambda" {
  source                  = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_cloudwatch?ref=v1.0.0"
  lambda_function_name    = "${module.lambda_sierra_window_generator.function_name}"
  lambda_function_arn     = "${module.lambda_sierra_window_generator.arn}"
  cloudwatch_trigger_arn  = "${aws_cloudwatch_event_rule.sierra_window_generator_rule.arn}"
  cloudwatch_trigger_name = "${aws_cloudwatch_event_rule.sierra_window_generator_rule.id}"
}

resource "aws_cloudwatch_event_rule" "sierra_window_generator_rule" {
  name                = "sierra_${var.resource_type}_window_generator_rule"
  description         = "Starts the sierra_window_generator lambda"
  schedule_expression = "rate(${var.lambda_trigger_minutes} minutes)"
}

resource "aws_iam_role_policy" "sierra_window_generator_sns_publish" {
  name   = "${module.lambda_sierra_window_generator.function_name}_policy"
  role   = "${module.lambda_sierra_window_generator.role_name}"
  policy = "${module.topic_sierra_windows.publish_policy}"
}
