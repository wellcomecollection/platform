resource "aws_iam_role_policy" "sierra_window_generator_sns_publish" {
  name   = "${module.window_generator_lambda.function_name}_policy"
  role   = "${module.window_generator_lambda.role_name}"
  policy = "${module.windows_topic.publish_policy}"
}

resource "aws_iam_role_policy" "allow_dynamo_access" {
  role   = "${module.sierra_to_dynamo_service.task_role_name}"
  policy = "${data.aws_iam_policy_document.sierra_table_permissions.json}"
}
