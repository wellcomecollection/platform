resource "aws_iam_role_policy" "register_ecs_task_allow_run_task" {
  role   = "${module.lambda_register_ecs_task.role_name}"
  policy = "${data.aws_iam_policy_document.allow_run_task.json}"
}

resource "aws_iam_role_policy" "register_ecs_task_requested_tasks_table" {
  role   = "${module.lambda_register_ecs_task.role_name}"
  policy = "${data.aws_iam_policy_document.tasks_table.json}"
}
