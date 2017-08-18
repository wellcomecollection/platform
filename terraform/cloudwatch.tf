resource "aws_cloudwatch_event_rule" "weekdays_at_7am" {
  name                = "weekdays_at_7am"
  description         = "Fires at 7am on weekdays"
  schedule_expression = "cron(0 7 ? * MON-FRI *)"
}

resource "aws_cloudwatch_event_rule" "every_5_minutes" {
  name                = "every_5_minutes"
  description         = "Fires every 5 minutes"
  schedule_expression = "rate(5 minutes)"
}

resource "aws_cloudwatch_event_rule" "every_minute" {
  name                = "every_minute"
  description         = "Fires every minute"
  schedule_expression = "rate(1 minute)"
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

# ECS Scheduled tasks

resource "aws_cloudwatch_event_target" "gatling_loris" {
  rule     = "${aws_cloudwatch_event_rule.every_5_minutes.name}"
  arn      = "${aws_ecs_cluster.services.id}"
  role_arn = "${aws_iam_role.scheduled_tasks_role.arn}"

  ecs_target {
    task_count          = 1
    task_definition_arn = "${module.gatling_loris.task_arn}"
  }
}

resource "aws_cloudwatch_event_target" "gatling_catalogue_api" {
  rule     = "${aws_cloudwatch_event_rule.every_5_minutes.name}"
  arn      = "${aws_ecs_cluster.services.id}"
  role_arn = "${aws_iam_role.scheduled_tasks_role.arn}"

  ecs_target {
    task_count          = 1
    task_definition_arn = "${module.gatling_catalogue_api.task_arn}"
  }
}

resource "aws_iam_role" "scheduled_tasks_role" {
  name               = "scheduled_tasks_role"
  assume_role_policy = "${data.aws_iam_policy_document.assume_scheduled_tasks_role.json}"
}

data "aws_iam_policy_document" "assume_scheduled_tasks_role" {
  statement {
    actions = [
      "sts:AssumeRole",
    ]

    principals {
      type        = "Service"
      identifiers = ["events.amazonaws.com"]
    }
  }
}
