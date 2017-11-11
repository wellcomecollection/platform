resource "aws_iam_role_policy" "task_tracking_describe_services" {
  role   = "${module.lambda_task_tracking.role_name}"
  policy = "${data.aws_iam_policy_document.describe_services.json}"
}

resource "aws_iam_role_policy" "task_tracking_tasks_table" {
  role   = "${module.lambda_task_tracking.role_name}"
  policy = "${data.aws_iam_policy_document.tasks_table.json}"
}

resource "aws_iam_role_policy" "dynamo_to_tasks_sns" {
  role   = "${module.lambda_dynamo_to_sns_tasks.role_name}"
  policy = "${module.task_updates_topic.publish_policy}"
}
