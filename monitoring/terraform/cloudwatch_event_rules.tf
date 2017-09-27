resource "aws_cloudwatch_event_rule" "every_minute" {
  name                = "every_minute_lambdas"
  description         = "Fires every minute"
  schedule_expression = "rate(1 minute)"
}
