locals {
  lambda_error_alarm_arn = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"

  vpc_id          = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_id}"
  private_subnets = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_private_subnets}"

  vhs_full_access_policy = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sierra_full_access_policy}"
  vhs_table_name         = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sierra_table_name}"
  vhs_bucket_name        = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sierra_bucket_name}"

  reindexed_items_topic_name = "${data.terraform_remote_state.shared_infra.catalogue_sierra_items_reindex_topic_name}"
}
