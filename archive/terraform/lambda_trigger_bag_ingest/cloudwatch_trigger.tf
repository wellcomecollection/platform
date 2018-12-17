resource "aws_cloudwatch_event_rule" "trigger_bag_ingest_every_fifteen_minutes" {
  name = "every_15_minutes_lambda_trigger_bag"
  description = "Trigger test bag ingest every fifteen minutes"
  schedule_expression = "rate(15 minutes)"
}

resource "aws_cloudwatch_event_target" "trigger_bag_ingest_every_fifteen_minutes" {
  rule = "${aws_cloudwatch_event_rule.trigger_bag_ingest_every_fifteen_minutes.name}"
  arn = "${aws_lambda_function.lambda_trigger_bag_ingest_monitoring.arn}"
  target_id = "trigger_bag_ingest"
}

resource "aws_lambda_permission" "allow_cloudwatch_to_call_bag_ingest" {
  statement_id = "AllowExecutionFromCloudWatch"
  action = "lambda:InvokeFunction"
  function_name = "${aws_lambda_function.lambda_trigger_bag_ingest_monitoring.function_name}"
  principal = "events.amazonaws.com"
  source_arn = "${aws_cloudwatch_event_rule.trigger_bag_ingest_every_fifteen_minutes.arn}"
}