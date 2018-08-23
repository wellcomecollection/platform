module "shard_generator_lambda" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v10.2.2"

  name      = "reindex_shard_generator_${var.vhs_table_name}"
  s3_bucket = "${var.infra_bucket}"
  s3_key    = "lambdas/reindexer/reindex_shard_generator.zip"

  module_name = "reindex_shard_generator"

  description = "Generate reindexShards for items in the ${var.vhs_table_name} table"

  timeout = 300

  environment_variables = {
    TABLE_NAME = "${var.vhs_table_name}"
    SOURCE_NAME = "${var.source_name}"
  }

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"

  log_retention_in_days = 30
}

module "trigger_shard_generator_lambda" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//lambda/trigger_dynamo?ref=v6.4.0"

  stream_arn    = "${var.vhs_table_stream_arn}"
  function_arn  = "${module.shard_generator_lambda.arn}"
  function_role = "${module.shard_generator_lambda.role_name}"

  batch_size = 50
}

resource "aws_iam_role_policy" "allow_shard_generator_put_vhs" {
  role   = "${module.shard_generator_lambda.role_name}"
  policy = "${var.vhs_table_update_policy}"
}
