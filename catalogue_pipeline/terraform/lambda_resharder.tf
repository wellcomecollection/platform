module "resharder_lambda" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v6.4.0"

  name   = "resharder"
  s3_key = "lambdas/catalogue_pipeline/reindexer_v2/resharder.zip"

  description = "Reshard S3 keys in the source-data table"

  timeout = 120

  environment_variables = {
    TABLE_NAME = "${module.versioned-hybrid-store.table_name}"
    S3_BUCKET  = "${module.versioned-hybrid-store.bucket_name}"
  }

  alarm_topic_arn = "${local.lambda_error_alarm_arn}"
}

module "trigger_resharder_lambda" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//lambda/trigger_dynamo?ref=v6.4.0"

  stream_arn    = "${module.versioned-hybrid-store.table_stream_arn}"
  function_arn  = "${module.resharder_lambda.arn}"
  function_role = "${module.resharder_lambda.role_name}"

  batch_size = 50
}

resource "aws_iam_role_policy" "allow_resharder_access" {
  role   = "${module.resharder_lambda.role_name}"
  policy = "${module.versioned-hybrid-store.full_access_policy}"
}
