resource "aws_iam_role_policy" "vhs_full_access_policy" {
  role   = "${module.sierra_merger_service.task_role_name}"
  policy = "${var.vhs_full_access_policy}"
}

resource "aws_iam_role_policy" "push_cloudwatch_metric" {
  role   = "${module.sierra_merger_service.task_role_name}"
  policy = "${data.aws_iam_policy_document.allow_cloudwatch_push_metrics.json}"
}
