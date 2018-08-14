locals {
  lambda_error_alarm_arn      = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"
  dlq_alarm_arn               = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
  vpc_id                      = "${data.terraform_remote_state.shared_infra.catalogue_vpc_id}"
  private_subnets             = "${data.terraform_remote_state.shared_infra.catalogue_private_subnets}"

  vhs_sierra_read_policy      = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sierra_read_policy}"
  vhs_sierra_bucket_name      = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sierra_bucket_name}"
  vhs_sierra_table_stream_arn = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sierra_table_stream_arn}"

  vhs_sourcedata_read_policy      = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sourcedata_read_policy}"
  vhs_sourcedata_bucket_name      = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sourcedata_bucket_name}"
  vhs_sourcedata_table_stream_arn = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sourcedata_table_stream_arn}"
}
