resource "aws_iam_role_policy" "post_to_slack_get_cloudwatch" {
  role   = "${module.lambda_post_to_slack.role_name}"
  policy = "${data.aws_iam_policy_document.cloudwatch_allow_filterlogs.json}"
}
