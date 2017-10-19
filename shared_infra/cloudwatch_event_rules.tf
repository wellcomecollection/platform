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

resource "aws_cloudwatch_event_rule" "ecs_container_instance_state_change" {
  name        = "ecs_container_instance_state_change"
  description = "Capture any ECS Container Instance state change"

  event_pattern = <<PATTERN
{
  "source": [
    "aws.ecs"
  ],
  "detail-type": [
    "ECS Container Instance State Change"
  ]
}
PATTERN
}
