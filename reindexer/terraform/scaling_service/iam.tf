data "aws_iam_policy_document" "read_from_q" {
  statement {
    actions = [
      "sqs:DeleteMessage",
      "sqs:ReceiveMessage",
    ]

    resources = [
      "${var.source_queue_arn}",
    ]
  }
}

resource "aws_iam_role_policy" "ecs_task_read_q" {
  name   = "ecs_${var.service_name}_read_${var.source_queue_name}_policy"
  role   = "${module.task.task_role_name}"
  policy = "${data.aws_iam_policy_document.read_from_q.json}"
}
