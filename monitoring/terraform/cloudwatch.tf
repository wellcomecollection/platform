resource "aws_cloudwatch_event_rule" "every_5_minutes" {
  name                = "every_5_minutes_lambdas"
  description         = "Fires every 5 minutes"
  schedule_expression = "rate(5 minutes)"
}
