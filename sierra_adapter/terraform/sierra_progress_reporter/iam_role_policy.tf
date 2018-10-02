resource "aws_iam_role_policy" "sierra_window_generator_sns_publish" {
  role   = "${module.window_generator_lambda.role_name}"
  policy = "${module.windows_topic.publish_policy}"
}
