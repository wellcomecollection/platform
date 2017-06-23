resource "aws_cloudwatch_event_rule" "weekdays_at_7am" {
  name                = "weekdays_at_7am"
  description         = "Fires at 7am on weekdays"
  schedule_expression = "cron(0 7 ? * MON-FRI *)"
}

resource "aws_cloudwatch_event_rule" "ecs_task_state_change" {
  name        = "ecs_task_state_change"
  description = "Capture any ECS Task state change"

  event_pattern = <<PATTERN
{
  "source": [
    "aws.ecs"
  ],
  "detail-type": [
    "ECS Task State Change"
  ]
}
PATTERN
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

