resource "aws_lambda_permission" "archive_asset_lookup_apigw" {
  statement_id  = "AllowExecutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = "${module.lambda_archive_bags.function_name}"
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.archive_asset_lookup.execution_arn}/prod/*/*"
}

module "lambda_archive_bags" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v11.5.0"

  name        = "archive_bags"
  description = "Serve requests for storage manifests"
  timeout     = 60
  memory_size = 1024

  environment_variables = {
    VHS_BUCKET_NAME = "${module.vhs_archive_manifest.bucket_name}"
    VHS_TABLE_NAME  = "${module.vhs_archive_manifest.table_name}"
    REGION          = "${var.aws_region}"
  }

  alarm_topic_arn = "${local.lambda_error_alarm_arn}"
  s3_bucket       = "${var.infra_bucket}"
  s3_key          = "lambdas/archive/archive_bags.zip"

  log_retention_in_days = 30
}

resource "aws_lambda_permission" "archive_ingest_apigw" {
  statement_id  = "AllowExecutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = "${module.lambda_archive_ingest.function_name}"
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.archive_asset_lookup.execution_arn}/prod/*/*"
}

module "lambda_archive_report_ingest_status" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v11.5.0"

  name        = "archive_report_ingest_status"
  description = "Report the status of ingest requests"
  timeout     = 60
  memory_size = 1024

  environment_variables = {
    TOPIC_ARN  = "${module.archivist_topic.arn}"
    TABLE_NAME = "${aws_dynamodb_table.archive_progress_table.name}"
    REGION     = "${var.aws_region}"
  }

  alarm_topic_arn = "${local.lambda_error_alarm_arn}"
  s3_bucket       = "${var.infra_bucket}"
  s3_key          = "lambdas/archive/archive_report_ingest_status.zip"

  log_retention_in_days = 30
}

module "lambda_archive_start_ingest" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v11.5.0"

  name        = "lambda_archive_start_ingest"
  description = "Receives POST messages that start the ingest process"
  timeout     = 60
  memory_size = 1024

  environment_variables = {
    TOPIC_ARN  = "${module.archivist_topic.arn}"
    TABLE_NAME = "${aws_dynamodb_table.archive_progress_table.name}"
    REGION     = "${var.aws_region}"
  }

  alarm_topic_arn = "${local.lambda_error_alarm_arn}"
  s3_bucket       = "${var.infra_bucket}"
  s3_key          = "lambdas/archive/archive_start_ingest.zip"

  log_retention_in_days = 30
}
