resource "aws_cloudwatch_event_rule" "window_generator_rule" {
  name                = "sierra_${var.resource_type}_window_generator_rule"
  description         = "Starts the sierra_window_generator lambda"
  schedule_expression = "rate(${var.trigger_interval_minutes} minutes)"
}
