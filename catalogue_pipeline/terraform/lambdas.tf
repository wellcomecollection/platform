module "complete_reindex_lambda" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.5"

  name   = "complete_reindex"
  s3_key = "lambdas/reindexer/complete_reindex.zip"

  description = "Mark reindex work as done in the reindex tracker table."

  timeout = 30

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
  s3_key = "lambdas/reindexer/reindex_shard_generator.zip"

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
