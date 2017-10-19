resource "aws_iam_role_policy" "notify_old_deploys_sns_publish" {
  role   = "${module.lambda_notify_old_deploys.role_name}"
  policy = "${module.old_deployments.publish_policy}"
}

resource "aws_iam_role_policy" "notify_old_deploys_deployments_table" {
  role   = "${module.lambda_notify_old_deploys.role_name}"
  policy = "${data.aws_iam_policy_document.deployments_table.json}"
}
