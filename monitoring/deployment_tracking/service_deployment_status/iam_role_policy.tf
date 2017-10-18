resource "aws_iam_role_policy" "service_deployment_status_describe_services" {
  role   = "${module.lambda_service_deployment_status.role_name}"
  policy = "${data.aws_iam_policy_document.describe_services.json}"
}

resource "aws_iam_role_policy" "service_deployment_status_deployments_table" {
  role   = "${module.lambda_service_deployment_status.role_name}"
  policy = "${data.aws_iam_policy_document.deployments_table.json}"
}
