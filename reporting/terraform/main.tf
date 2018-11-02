module "lambda_transformer_example" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v10.2.2"

  name        = "transformer_example"
  description = "Transform source data and send to ES."
  timeout     = 25

  environment_variables = {
    ES_URL      = "${var.reporting_es_url}"
    ES_USER     = "${var.reporting_es_user}"
    ES_PASS     = "${var.reporting_es_pass}"
    ES_INDEX    = "test_index_example"
    ES_DOC_TYPE = "example"
  }

  alarm_topic_arn = "${local.lambda_error_alarm_arn}"

  s3_bucket = "${local.infra_bucket}"
  s3_key    = "lambdas/reporting/transformer_example.zip"

  log_retention_in_days = 30
}

module "trigger_miro_topic_transformer_example" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v10.2.2"

  lambda_function_name = "${module.lambda_transformer_example.function_name}"
  sns_trigger_arn      = "${local.miro_topic_arn}"
  lambda_function_arn  = "${module.lambda_transformer_example.arn}"
}

module "trigger_sierra_topic_transformer_example" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v10.2.2"

  lambda_function_name = "${module.lambda_transformer_example.function_name}"
  sns_trigger_arn      = "${local.sierra_topic_arn}"
  lambda_function_arn  = "${module.lambda_transformer_example.arn}"
}
