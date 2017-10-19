resource "aws_iam_role_policy" "dynamo_to_miro_sns" {
  role   = "${module.lambda_dynamo_to_sns.role_name}"
  policy = "${var.miro_transformer_topic_publish_policy}"
}

resource "aws_iam_role_policy" "dynamo_to_calm_sns" {
  role   = "${module.lambda_dynamo_to_sns.role_name}"
  policy = "${var.calm_transformer_topic_publish_policy}"
}
