resource "aws_lambda_permission" "archive_asset_lookup_apigw" {
  statement_id  = "AllowExecutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = "${module.lambda_archive_asset_lookup.function_name}"
  principal     = "apigateway.amazonaws.com"

  source_arn = "${aws_api_gateway_deployment.test.execution_arn}/*/*"
}

module "lambda_archive_asset_lookup" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v11.5.0"

  name        = "archive_asset_lookup_${var.name}"
  module_name = "archive_asset_lookup"
  description = "Serve requests for storage manifests"
  timeout     = 60
  memory_size = 1024

  environment_variables = {
    TABLE_NAME  = "${data.aws_dynamodb_table.storage_manifest.name}"
    BUCKET_NAME = "${data.aws_s3_bucket.storage_manifests.bucket}"
    AWS_REGION  = "${var.aws_region}"
  }

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"
  s3_bucket       = "${var.infra_bucket}"
  s3_key          = "lambdas/shared_infra/archive_asset_lookup.zip"

  log_retention_in_days = 30
}
