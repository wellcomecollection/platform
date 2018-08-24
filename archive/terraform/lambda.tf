resource "aws_lambda_permission" "archive_asset_lookup_apigw" {
  statement_id  = "AllowExecutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = "${module.lambda_archive_asset_lookup.function_name}"
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.archive_asset_lookup.execution_arn}/prod/*/*"
}

module "lambda_archive_asset_lookup" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v11.5.0"

  name        = "archive_asset_lookup"
  module_name = "endpoint"
  description = "Serve requests for storage manifests"
  timeout     = 60
  memory_size = 1024

  environment_variables = {
    BUCKET_NAME = "${module.vhs_archive_manifest.bucket_name}"
    TABLE_NAME  = "${module.vhs_archive_manifest.table_name}"
    REGION      = "${var.aws_region}"
  }

  alarm_topic_arn = "${local.lambda_error_alarm_arn}"
  s3_bucket       = "${var.infra_bucket}"
  s3_key          = "lambdas/archive/archive_asset_lookup.zip"

  log_retention_in_days = 30
}

resource "aws_lambda_permission" "archive_ingest_apigw" {
  statement_id  = "AllowExecutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = "${module.lambda_archive_ingest.function_name}"
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.archive_asset_lookup.execution_arn}/prod/*/*"
}

module "lambda_archive_ingest" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v11.5.0"

  name          = "archive_ingest"
  description   = "Receive ingest requests"
  timeout       = 60
  memory_size   = 1024

  environment_variables = {
    TOPIC_ARN = "${module.archivist_topic.arn}"
    REGION    = "${var.aws_region}"
  }

  alarm_topic_arn = "${local.lambda_error_alarm_arn}"
  s3_bucket       = "${var.infra_bucket}"
  s3_key          = "lambdas/archive/archive_ingest.zip"

  log_retention_in_days = 30
}
