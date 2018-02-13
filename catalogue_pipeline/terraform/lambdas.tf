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

  alarm_topic_arn = "${local.lambda_error_alarm_arn}"
}

module "trigger_reindex_job_creator_lambda" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//lambda/trigger_sns?ref=v1.0.0"

  lambda_function_name = "${module.reindex_job_creator_lambda.function_name}"
  lambda_function_arn  = "${module.reindex_job_creator_lambda.arn}"

  sns_trigger_arn = "${module.reindex_shard_tracker_topic.arn}"
}

# Role policies for the reindex_job_creator

resource "aws_iam_role_policy" "reindex_job_creator_lambda_sns" {
  role   = "${module.reindex_job_creator_lambda.role_name}"
  policy = "${module.reindex_jobs_topic.publish_policy}"
}

module "complete_reindex_lambda" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.5"

  name   = "complete_reindex"
  s3_key = "lambdas/catalogue_pipeline/reindex_job_creator.zip"

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

  sns_trigger_arn = "${data.aws_sns_topic.reindex_jobs_complete_topic.arn}"
}

# Role policies for the complete_reindex_lambda

resource "aws_iam_role_policy" "complete_reindex_lambda_reindexer_tracker_table" {
  role   = "${module.complete_reindex_lambda.role_name}"
  policy = "${data.aws_iam_policy_document.reindex_tracker_table.json}"
}
