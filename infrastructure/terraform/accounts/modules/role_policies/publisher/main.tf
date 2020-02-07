resource "aws_iam_role_policy" "ecr_power_user" {
  role   = var.role_name
  policy = data.aws_iam_policy_document.ecr_power_user.json
}

resource "aws_iam_role_policy" "ssm_parameter_store_power_user" {
  role   = var.role_name
  policy = data.aws_iam_policy_document.ssm_parameter_store_power_user.json
}
