resource "aws_iam_role_policy" "task_status_change_to_sns" {
  role   = "${module.lambda_task_status_notifier.role_name}"
  policy = "${module.task_status_change_topic.publish_policy}"
}
