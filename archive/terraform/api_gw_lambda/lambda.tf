resource "aws_lambda_permission" "archive_asset_lookup_apigw" {
  statement_id = "AllowExecutionFromAPIGateway"
  action = "lambda:InvokeFunction"
  function_name = "${module.lambda.function_name}"
  principal = "apigateway.amazonaws.com"

  source_arn = "${aws_api_gateway_deployment.test.execution_arn}/*/*"
}

module "lambda" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v11.5.0"

  name = "${var.name}"
  module_name = "${var.name}"
  description = "${var.description}"

  timeout = "${var.timeout}"
  memory_size = "${var.memory_size}"

  environment_variables = "${var.environment_variables}"

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
  s3_bucket = "${var.infra_bucket}"
  s3_key = "lambdas/archive/archive_asset_lookup.zip"

  log_retention_in_days = 30
}
