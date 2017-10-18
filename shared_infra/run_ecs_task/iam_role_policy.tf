resource "aws_iam_role_policy" "notify_old_deploys_deployments_table" {
  role   = "${module.lambda_run_ecs_task.role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}
