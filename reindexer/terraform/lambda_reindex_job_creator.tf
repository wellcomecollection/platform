module "reindex_job_creator_lambda" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v1.0.5"

  name      = "reindex_job_creator"
  s3_bucket = "${var.infra_bucket}"
  s3_key    = "lambdas/reindexer/reindex_job_creator.zip"

  description = "Generate jobs for the reindexer from the ${aws_dynamodb_table.reindex_shard_tracker.id} table"

  timeout = 10

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

resource "aws_iam_role_policy" "reindex_job_creator_lambda_sns" {
  role   = "${module.reindex_job_creator_lambda.role_name}"
  policy = "${module.reindex_jobs_topic.publish_policy}"
}
