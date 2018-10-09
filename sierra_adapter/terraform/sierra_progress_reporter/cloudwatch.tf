resource "aws_cloudwatch_event_rule" "rule" {
  name                = "sierra_progress_reporter_rule"
  description         = "Starts the sierra_progress_reporter Lambda"
  schedule_expression = "rate(${var.trigger_interval_minutes} minutes)"
}
