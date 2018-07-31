module "sourcedata_reindex_shard_generator" {
  source = "reindex_shard_generator"

  vhs_table_name          = "${local.vhs_sourcedata_table_name}"
  vhs_table_stream_arn    = "${local.vhs_sourcedata_table_stream_arn}"
  vhs_table_update_policy = "${local.vhs_sourcedata_dynamodb_update_policy}"

  infra_bucket           = "${var.infra_bucket}"
  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
}
