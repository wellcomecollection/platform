resource "aws_iam_role_policy" "lambda_gatling_to_cloudwatch_put_metric" {
  role   = "${module.lambda_gatling_to_cloudwatch.role_name}"
  policy = "${var.allow_cloudwatch_push_metrics_json}"
}
