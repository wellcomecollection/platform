resource "aws_lambda_permission" "lambda_permission" {
  statement_id  = "AllowExecutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = "${module.lambda.function_name}"
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${var.api_gateway_execution_arn}/prod/*/*"
}

module "lambda" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v11.5.0"

  name        = "${var.name}"
  description = "${var.description}"
  timeout     = 60
  memory_size = 1024

  environment_variables = "${var.environment_variables}"

  alarm_topic_arn = "${local.lambda_error_alarm_arn}"
  s3_bucket       = "${local.infra_bucket}"
  s3_key          = "lambdas/archive/${var.name}.zip"

  log_retention_in_days = 30
}
