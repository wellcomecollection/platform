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
  s3_key = "lambdas/catalogue_pipeline/reindexer_v2/reindex_job_creator.zip"

  description = "Generate jobs for the reindexer from the ${aws_dynamodb_table.reindex_shard_tracker.id} table"

  environment_variables = {
    TOPIC_ARN = "${module.reindex_jobs_topic.arn}"
  }

  alarm_topic_arn = "${local.lambda_error_alarm_arn}"
}

module "trigger_reindex_job_creator_lambda" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//lambda/trigger_sns?ref=v1.0.0"

  lambda_function_name = "${module.reindex_job_creator_lambda.function_name}"
  lambda_function_arn  = "${module.reindex_job_creator_lambda.arn}"

  sns_trigger_arn = "${module.reindex_shard_tracker_topic.arn}"
}

module "complete_reindex_lambda" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.5"

  name   = "complete_reindex"
  s3_key = "lambdas/catalogue_pipeline/reindexer_v2/complete_reindex.zip"

  description = "Mark reindex work as done in the reindex tracker table."

  environment_variables = {
    TABLE_NAME = "${aws_dynamodb_table.reindex_shard_tracker.name}"
  }

  alarm_topic_arn = "${local.lambda_error_alarm_arn}"
}

module "trigger_complete_reindex_lambda" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//lambda/trigger_sns?ref=v1.0.0"

  lambda_function_name = "${module.complete_reindex_lambda.function_name}"
  lambda_function_arn  = "${module.complete_reindex_lambda.arn}"

  sns_trigger_arn = "${module.reindex_jobs_complete_topic.arn}"
}

module "shard_generator_lambda" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v6.4.0"

  name   = "reindex_shard_generator"
  s3_key = "lambdas/catalogue_pipeline/reindexer_v2/reindex_shard_generator.zip"

  description = "Generate reindexShards for items in the ${module.versioned-hybrid-store.table_name} table"

  timeout = 60

  environment_variables = {
    TABLE_NAME = "${module.versioned-hybrid-store.table_name}"
  }

  alarm_topic_arn = "${local.lambda_error_alarm_arn}"
}

module "trigger_shard_generator_lambda" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//lambda/trigger_dynamo?ref=v6.4.0"

  stream_arn    = "${module.versioned-hybrid-store.table_stream_arn}"
  function_arn  = "${module.shard_generator_lambda.arn}"
  function_role = "${module.shard_generator_lambda.role_name}"

  batch_size = 50
}
