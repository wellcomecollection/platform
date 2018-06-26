locals {

  lambda_error_alarm_arn     = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"

  vhs_full_access_policy = "${data.terraform_remote_state.catalogue_pipeline.vhs_sourcedata_full_access_policy}"
  vhs_table_name         = "${data.terraform_remote_state.catalogue_pipeline.vhs_sourcedata_table_name}"
  vhs_bucket_name        = "${data.terraform_remote_state.catalogue_pipeline.vhs_sourcedata_bucket_name}"
}