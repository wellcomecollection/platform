module "lambda_archive_bags" {
  source = "apigw_lambda"

  name        = "archive_bags"
  description = "Serve requests for storage manifests"

  environment_variables = {
    VHS_BUCKET_NAME = "${module.vhs_archive_manifest.bucket_name}"
    VHS_TABLE_NAME  = "${module.vhs_archive_manifest.table_name}"
    REGION          = "${var.aws_region}"
  }
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
}

module "lambda_archive_start_ingest" {
  source = "apigw_lambda"

  name        = "lambda_archive_start_ingest"
  description = "Receives POST messages that start the ingest process"

  environment_variables = {
    TOPIC_ARN  = "${module.archivist_topic.arn}"
    TABLE_NAME = "${aws_dynamodb_table.archive_progress_table.name}"
    REGION     = "${var.aws_region}"
  }
}
