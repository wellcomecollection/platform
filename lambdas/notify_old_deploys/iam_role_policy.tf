resource "aws_iam_role_policy" "notify_old_deploys_sns_publish" {
  role   = "${module.lambda_notify_old_deploys.role_name}"
  policy = "${var.old_deployments_publish_policy}"
}

resource "aws_iam_role_policy" "notify_old_deploys_deployments_table" {
  role   = "${module.lambda_notify_old_deploys.role_name}"
  policy = "${var.iam_policy_document_deployments_table_json}"
}
