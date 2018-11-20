module "lambda_dynamo_write_heartbeat" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v10.2.2"

  name        = "${var.name}"
  module_name = "dynamo_write_heartbeat"
  description = "Keep DynamoDB capacity scaling by sending heartbeat writes"
  timeout     = 30
  memory_size = 128

  environment_variables = {
    TABLE_NAMES = "${join(",", var.dynamo_table_names)}"
  }

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
  s3_bucket       = "${var.infra_bucket}"
  s3_key          = "lambdas/infrastructure/critical/lambdas/dynamo_write_heartbeat.zip"

  log_retention_in_days = 30
}

resource "aws_cloudwatch_event_rule" "dynamo_heartbeat_scheduler_rule" {
  name                = "${var.name}-rule"
  description         = "Heartbeat scheduler for writes to dynamoDb"
  schedule_expression = "rate(10 minutes)"
}

module "trigger_dynamo_heartbeat_scheduler_lambda" {
  source                  = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_cloudwatch?ref=v1.0.0"
  lambda_function_name    = "${module.lambda_dynamo_write_heartbeat.function_name}"
  lambda_function_arn     = "${module.lambda_dynamo_write_heartbeat.arn}"
  cloudwatch_trigger_arn  = "${aws_cloudwatch_event_rule.dynamo_heartbeat_scheduler_rule.arn}"
  cloudwatch_trigger_name = "${aws_cloudwatch_event_rule.dynamo_heartbeat_scheduler_rule.id}"
}

resource "aws_iam_role_policy" "dynamo_heartbeat_scheduler_lambda" {
  role   = "${module.lambda_dynamo_write_heartbeat.role_name}"
  policy = "${data.aws_iam_policy_document.allow_dynamodb_delete_item.json}"
}
