resource "aws_iam_role_policy" "lambda_update_dynamo_capacity_table_access" {
  role   = "${module.lambda_update_dynamo_capacity.role_name}"
  policy = "${data.aws_iam_policy_document.allow_table_capacity_changes.json}"
}
