module "lambda_dynamo_write_heartbeat" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v10.2.2"

  name        = "dynamo_write_heartbeat_${var.name}"
  module_name = "dynamo_write_heartbeat"
  description = "Keep DynamoDb capacity scaling by sending heartbeat writes"
  timeout     = 5
  memory_size = 256

  environment_variables = {
    TABLE_NAMES  = "${var.dynamo_table_names}"
  }

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
  s3_bucket       = "${var.infra_bucket}"
  s3_key          = "lambdas/shared_infra/dynamo_write_heartbeat.zip"

  log_retention_in_days = 30
}

resource "aws_cloudwatch_event_rule" "every_ten_minutes" {
  name = "every-ten-minutes"
  description = "Fire every ten minutes"
  schedule_expression = "rate(10 minutes)"
}

resource "aws_cloudwatch_event_target" "dynamo_write_heartbeat_every_ten_minutes" {
  rule = "${aws_cloudwatch_event_rule.every_ten_minutes.name}"
  target_id = "dynamo_write_heartbeat"
  arn = "${module.lambda_dynamo_write_heartbeat.arn}"
}

resource "aws_lambda_permission" "allow_cloudwatch_to_call_dynamo_write_heartbeat" {
  statement_id = "AllowExecutionFromCloudWatch"
  action = "lambda:InvokeFunction"
  function_name = "${module.lambda_dynamo_write_heartbeat.function_name}"
  principal = "events.amazonaws.com"
  source_arn = "${aws_cloudwatch_event_rule.every_ten_minutes.arn}"
}