resource "aws_iam_role_policy" "lambda_allow_update_api_size_sns_publish" {
  role   = "${module.lambda_update_api_size.role_name}"
  policy = "${data.aws_iam_policy_document.publish_to_service_scheduler_topic.json}"
}

resource "aws_iam_role_policy" "lambda_allow_update_api_size_s3_read" {
  role   = "${module.lambda_update_api_size.role_name}"
  policy = "${data.aws_iam_policy_document.allow_s3_read_prod_api.json}"
}
