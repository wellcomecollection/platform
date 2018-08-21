resource "aws_cloudwatch_event_rule" "snapshot_scheduler_rule" {
  name                = "snapshot_scheduler_rule"
  description         = "Starts the snapshot_scheduler lambda"
  schedule_expression = "rate(1 day)"
}
