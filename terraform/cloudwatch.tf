resource "aws_cloudwatch_event_rule" "daily_2am" {
  name                = "daily_2am"
  description         = "Fires once a day at 2am"
  schedule_expression = "cron(* 2 * * *)"
}
