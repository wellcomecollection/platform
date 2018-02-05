module "reindex_shard_tracker_dynamo_to_sns" {
  source = "git::https://github.com/wellcometrust/platform.git//shared_infra/dynamo_to_sns"

  name           = "reindex_shard_tracker_updates"
  src_stream_arn = "${aws_dynamodb_table.reindex_shard_tracker.stream_arn}"
  dst_topic_arn  = "${module.reindex_shard_tracker_topic.arn}"

  stream_view_type = "NEW_IMAGE_ONLY"

  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
}

module "reindex_job_creator_lambda" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.5"

  name   = "reindex_job_creator"
  s3_key = "lambdas/catalogue_pipeline/reindex_job_creator.zip"

  description = "Generate jobs for the reindexer from the ${aws_dynamodb_table.reindex_shard_tracker.id} table"

  environment_variables = {
    TOPIC_ARN = "${module.reindex_jobs_topic.arn}"
  }

  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
}

module "trigger_reindex_job_creator_lambda" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//lambda/trigger_sns?ref=v1.0.0"

  lambda_function_name = "${module.reindex_job_creator_lambda.function_name}"
  lambda_function_arn  = "${module.reindex_job_creator_lambda.arn}"
  sns_trigger_arn      = "${module.reindex_shard_tracker_topic.arn}"
}
