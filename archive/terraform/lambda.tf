module "lambda_archive_bags" {
  source = "apigw_lambda"

  name        = "archive_bags"
  description = "Serve requests for storage manifests"

  environment_variables = {
    VHS_BUCKET_NAME = "${module.vhs_archive_manifest.bucket_name}"
    VHS_TABLE_NAME  = "${module.vhs_archive_manifest.table_name}"
    REGION          = "${var.aws_region}"
  }

  api_gateway_execution_arn = "${aws_api_gateway_rest_api.archive_asset_lookup.execution_arn}"
}

module "lambda_archive_report_ingest_status" {
  source = "apigw_lambda"

  name        = "archive_report_ingest_status"
  description = "Report the status of ingest requests"

  environment_variables = {
    TOPIC_ARN  = "${module.archivist_topic.arn}"
    TABLE_NAME = "${aws_dynamodb_table.archive_progress_table.name}"
    REGION     = "${var.aws_region}"
  }

  api_gateway_execution_arn = "${aws_api_gateway_rest_api.archive_asset_lookup.execution_arn}"
}

module "lambda_archive_request_ingest" {
  source = "apigw_lambda"

  name        = "archive_request_ingest"
  description = "Receives POST messages that request a new ingest process"

  environment_variables = {
    TOPIC_ARN  = "${module.archivist_topic.arn}"
    TABLE_NAME = "${aws_dynamodb_table.archive_progress_table.name}"
    REGION     = "${var.aws_region}"
  }

  api_gateway_execution_arn = "${aws_api_gateway_rest_api.archive_asset_lookup.execution_arn}"
}
