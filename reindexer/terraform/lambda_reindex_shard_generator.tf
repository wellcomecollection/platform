module "miro_reindex_shard_generator" {
  source = "reindex_shard_generator"

  source_name             = "miro"
  vhs_table_name          = "${local.vhs_miro_table_name}"
  vhs_table_update_policy = "${local.vhs_miro_dynamodb_update_policy}"

  infra_bucket           = "${var.infra_bucket}"
  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
}

module "sierra_reindex_shard_generator" {
  source = "reindex_shard_generator"

  source_name             = "sierra"
  vhs_table_name          = "${local.vhs_sierra_table_name}"
  vhs_table_update_policy = "${local.vhs_sierra_dynamodb_update_policy}"

  infra_bucket           = "${var.infra_bucket}"
  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"
}
