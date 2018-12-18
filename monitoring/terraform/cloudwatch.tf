resource "aws_cloudwatch_event_rule" "every_5_minutes" {
  name                = "every_5_minutes_lambdas"
  description         = "Fires every 5 minutes"
  schedule_expression = "rate(5 minutes)"
}

resource "aws_cloudwatch_event_rule" "every_minute" {
  name                = "every_minute_lambdas"
  description         = "Fires every minute"
  schedule_expression = "rate(1 minute)"
}

resource "aws_cloudwatch_event_rule" "every_day_at_8am" {
  name                = "every_day_at_8am"
  schedule_expression = "cron(0 8 * * ? *)"
}
