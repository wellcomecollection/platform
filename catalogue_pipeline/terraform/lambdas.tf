module "lambda_schedule_reindexer" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.0"

  name        = "schedule_reindexer"
  description = "Schedules the reindexer based on the ReindexerTracker table"

  environment_variables = {
    SCHEDULER_TOPIC_ARN = "${local.service_scheduler_topic_arn}"
    DYNAMO_TABLE_NAME   = "${aws_dynamodb_table.miro_table.name}"
    CLUSTER_NAME        = "${module.catalogue_pipeline_cluster.cluster_name}"
    REINDEXERS          = "${aws_dynamodb_table.miro_table.name}=miro_reindexer"
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
