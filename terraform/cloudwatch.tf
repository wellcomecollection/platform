resource "aws_cloudwatch_event_rule" "once_a_day" {
  name                = "once-a-day"
  description         = "Fires once a day"
  schedule_expression = "rate(1 day)"
}
