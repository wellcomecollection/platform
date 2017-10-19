resource "aws_iam_role_policy" "update_tasks_for_config_change_policy" {
  role   = "${module.lambda_update_task_for_config_change.role_name}"
  policy = "${data.aws_iam_policy_document.stop_running_tasks.json}"
}
