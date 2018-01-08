resource "aws_iam_role_policy" "sierra_window_generator_sns_publish" {
  role   = "${module.window_generator_lambda.role_name}"
  policy = "${module.windows_topic.publish_policy}"
}

resource "aws_iam_role_policy" "allow_dynamo_access" {
  role   = "${module.sierra_to_dynamo_service.task_role_name}"
  policy = "${data.aws_iam_policy_document.sierra_table_permissions.json}"
}

resource "aws_iam_role_policy" "allow_merged_dynamo_access" {
  role   = "${module.sierra_merger_service.task_role_name}"
  policy = "${data.aws_iam_policy_document.sierra_merged_table_permissions.json}"
}

resource "aws_iam_role_policy" "allow_read_from_windows_q" {
  role   = "${module.sierra_to_dynamo_service.task_role_name}"
  policy = "${data.aws_iam_policy_document.read_from_windows_q.json}"
}
