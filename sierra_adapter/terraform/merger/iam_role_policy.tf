resource "aws_iam_role_policy" "allow_merged_dynamo_access" {
  role   = "${module.sierra_merger_service.task_role_name}"
  policy = "${data.aws_iam_policy_document.sierra_merged_table_permissions.json}"
}
