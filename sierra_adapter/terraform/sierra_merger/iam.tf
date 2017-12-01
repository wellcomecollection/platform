module "ecs_sierra_merger" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs_iam?ref=v1.0.0"
  name   = "sierra_${var.resource_type}_merger"
}

resource "aws_iam_role_policy" "allow_read_from_windows_queue" {
  role   = "${module.ecs_sierra_merger.task_role_name}"
  policy = "${module.update_events_queue.read_policy}"
}

data "aws_iam_policy_document" "sierra_table_permissions" {
  statement {
    actions = [
      "dynamodb:*",
    ]

    resources = [
      "${var.target_dynamo_table_arn}",
    ]
  }
}

resource "aws_iam_role_policy" "allow_dynamo_access" {
  role   = "${module.ecs_sierra_merger.task_role_name}"
  policy = "${data.aws_iam_policy_document.sierra_table_permissions.json}"
}
