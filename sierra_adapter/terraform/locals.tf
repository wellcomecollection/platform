locals {
  lambda_error_alarm_arn = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"

  vpc_id          = "${data.terraform_remote_state.shared_infra.catalogue_vpc_id}"
  public_subnets  = "${data.terraform_remote_state.shared_infra.catalogue_public_subnets}"
  private_subnets = "${data.terraform_remote_state.shared_infra.catalogue_private_subnets}"

  vhs_full_access_policy = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sierra_full_access_policy}"
  vhs_table_name         = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sierra_table_name}"
  vhs_bucket_name        = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sierra_bucket_name}"
}
