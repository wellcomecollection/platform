module "shard_generator_lambda" {
  source = "git::https://github.com/wellcometrust/terraform.git//lambda?ref=v6.4.0"

  name   = "reindex_shard_generator"
  s3_key = "lambdas/reindexer/reindex_shard_generator.zip"

  description = "Generate reindexShards for items in the ${local.vhs_table_name} table"

  timeout = 60

  environment_variables = {
    TABLE_NAME = "${local.vhs_table_name}"
  }

  alarm_topic_arn = "${local.lambda_error_alarm_arn}"
}

module "trigger_shard_generator_lambda" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//lambda/trigger_dynamo?ref=v6.4.0"

  stream_arn    = "${local.vhs_table_stream_arn}"
  function_arn  = "${module.shard_generator_lambda.arn}"
  function_role = "${module.shard_generator_lambda.role_name}"

  batch_size = 50
}

resource "aws_iam_role_policy" "allow_shard_generator_put_vhs" {
  role   = "${module.shard_generator_lambda.role_name}"
  policy = "${local.vhs_dynamo_update_policy}"
}
