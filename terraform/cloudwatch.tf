resource "aws_cloudwatch_event_rule" "weekdays_at_7am" {
  name                = "weekdays_at_7am"
  description         = "Fires at 7am on weekdays"
  schedule_expression = "cron(0 7 ? * MON-FRI *)"
}
