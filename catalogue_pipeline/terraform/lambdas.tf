module "lambda_schedule_reindexer" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.0"

  name        = "schedule_reindexer"
  description = "Schedules the reindexer based on the ReindexerTracker table"

  environment_variables = {
    SCHEDULER_TOPIC_ARN     = "${local.service_scheduler_topic_arn}"
    DYNAMO_TABLE_NAME       = "${aws_dynamodb_table.miro_table.name}"
    DYNAMO_TOPIC_ARN        = "${local.dynamo_capacity_topic_arn}"
    DYNAMO_DESIRED_CAPACITY = "125"
    CLUSTER_NAME            = "${aws_ecs_cluster.services.name}"
    REINDEXERS              = "${aws_dynamodb_table.miro_table.name}=miro_reindexer"
  }

  alarm_topic_arn = "${local.lambda_error_alarm_arn}"
  s3_key          = "lambdas/catalogue_pipeline/schedule_reindexer.zip"
}

module "trigger_reindexer_lambda" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_dynamo?ref=v1.0.0"

  stream_arn    = "${aws_dynamodb_table.reindex_tracker.stream_arn}"
  function_arn  = "${module.lambda_schedule_reindexer.arn}"
  function_role = "${module.lambda_schedule_reindexer.role_name}"
}

module "dynamo_to_sns" {
  source = "git::https://github.com/wellcometrust/platform.git//shared_infra/dynamo_to_sns?ref=dynamo_to_sns-redux"

  name           = "MiroData"
  src_stream_arn = "${aws_dynamodb_table.miro_table.stream_arn}"
  dst_topic_arn  = "${module.miro_transformer_prefilter_topic.arn}"

  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
}

module "lambda_miro_transformer_filter" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.2.0"

  name        = "miro_transformer_filter"
  module_name = "transformer_sns_filter"
  description = "Filters DynamoDB events for the transformer"

  environment_variables = {
    TOPIC_ARN = "${module.miro_transformer_topic.arn}"
  }

  alarm_topic_arn = "${local.lambda_error_alarm_arn}"
  s3_key          = "lambdas/catalogue_pipeline/transformer_sns_filter.zip"
}

module "trigger_miro_transformer_filter" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda/trigger_sns?ref=v1.0.0"

  sns_trigger_arn      = "${module.miro_transformer_prefilter_topic.arn}"
  lambda_function_arn  = "${module.lambda_miro_transformer_filter.arn}"
  lambda_function_name = "${module.lambda_miro_transformer_filter.role_name}"
}
