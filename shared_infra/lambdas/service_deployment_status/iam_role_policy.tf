resource "aws_iam_role_policy" "service_deployment_status_describe_services" {
  role   = "${module.lambda_service_deployment_status.role_name}"
  policy = "${var.iam_policy_document_describe_services_json}"
}

resource "aws_iam_role_policy" "service_deployment_status_deployments_table" {
  role   = "${module.lambda_service_deployment_status.role_name}"
  policy = "${var.iam_policy_document_deployments_table_json}"
}
