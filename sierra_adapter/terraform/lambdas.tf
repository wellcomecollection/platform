module "lambda_sierra_bibs_merger_filter" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.2.0"

  name        = "sierra_bibs_merger_filter_filter"
  module_name = "transformer_sns_filter"
  description = "Filters DynamoDB events for the Sierra bibs merger"

  environment_variables = {
    TOPIC_ARN = "${module.sierra_bib_merger_events_topic.arn}"
  }

  alarm_topic_arn = "${local.lambda_error_alarm_arn}"
  s3_key          = "lambdas/catalogue_pipeline/transformer_sns_filter.zip"
}

module "trigger_sierra_bibs_merger_filter" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"

  sns_trigger_arn      = "${module.sierra_to_dynamo_bibs.topic_arn}"
  lambda_function_arn  = "${module.lambda_sierra_bibs_merger_filter.arn}"
  lambda_function_name = "${module.lambda_sierra_bibs_merger_filter.role_name}"
}
