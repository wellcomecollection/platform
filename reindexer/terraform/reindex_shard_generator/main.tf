module "shard_generator_lambda" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v10.2.2"

  name      = "reindex_shard_generator_${var.vhs_table_name}"
  s3_bucket = "${var.infra_bucket}"
  s3_key    = "lambdas/reindexer/reindex_shard_generator.zip"

  module_name = "reindex_shard_generator"

  description = "Generate reindexShards for items in the ${var.vhs_table_name} table"

  timeout = 300

  environment_variables = {
    TABLE_NAME  = "${var.vhs_table_name}"
    SOURCE_NAME = "${var.source_name}"
  }

  alarm_topic_arn = "${var.lambda_error_alarm_arn}"

  log_retention_in_days = 30
}

resource "aws_iam_role_policy" "allow_shard_generator_put_vhs" {
  role   = "${module.shard_generator_lambda.role_name}"
  policy = "${var.vhs_table_update_policy}"
}
