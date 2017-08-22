resource "aws_cloudwatch_event_target" "scheduled_task" {
  rule     = "${var.cloudwatch_event_rule_name}"
  arn      = "${var.cluster_arn}"
  role_arn = "${aws_iam_role.scheduled_task_role.arn}"

  ecs_target {
    task_count          = 1
    task_definition_arn = "${var.task_definition_arn}"
  }
}

resource "aws_iam_role" "scheduled_task_role" {
  assume_role_policy = "${data.aws_iam_policy_document.assume_scheduled_task_role.json}"
}

data "aws_iam_policy_document" "assume_scheduled_task_role" {
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

data "aws_iam_policy_document" "scheduled_task_runtask_role" {
  statement {
    actions = [
      "ecs:RunTask",
    ]

    resources = [
      "*",
    ]
  }
}

resource "aws_iam_role_policy" "scheduled_task_runtask_role_policy" {
  role   = "${aws_iam_role.scheduled_task_role.name}"
  policy = "${data.aws_iam_policy_document.scheduled_task_runtask_role.json}"
}
