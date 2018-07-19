locals {
  vhs_full_access_policy     = "${data.terraform_remote_state.catalogue_pipeline.vhs_sourcedata_full_access_policy}"
  vhs_dynamodb_update_policy = "${data.terraform_remote_state.catalogue_pipeline.vhs_sourcedata_dynamodb_update_policy}"
  vhs_table_name             = "${data.terraform_remote_state.catalogue_pipeline.vhs_sourcedata_table_name}"
  vhs_table_stream_arn       = "${data.terraform_remote_state.catalogue_pipeline.vhs_sourcedata_table_stream_arn}"

  vpc_id          = "${data.terraform_remote_state.shared_infra.catalogue_vpc_id}"
  private_subnets = "${data.terraform_remote_state.shared_infra.catalogue_private_subnets}"

  lambda_error_alarm_arn = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"
  dlq_alarm_arn          = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"

  reindex_request_creator_container_image   = "${module.ecr_repository_reindex_request_creator.repository_url}:${var.release_ids["reindex_request_creator"]}"
  reindex_request_processor_container_image = "${module.ecr_repository_reindex_request_processor.repository_url}:${var.release_ids["reindex_request_processor"]}"
}
