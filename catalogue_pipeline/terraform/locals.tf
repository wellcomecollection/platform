locals {
  lambda_error_alarm_arn             = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"
  dlq_alarm_arn                      = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
  vpc_id                             = "${data.terraform_remote_state.shared_infra.catalogue_vpc_id}"
  private_subnets                    = "${data.terraform_remote_state.shared_infra.catalogue_private_subnets}"
  transformer_miro_container_image   = "${module.ecr_repository_transformer_miro.repository_url}:${var.release_ids["transformer_miro"]}"
  transformer_sierra_container_image = "${module.ecr_repository_transformer_sierra.repository_url}:${var.release_ids["transformer_sierra"]}"
  recorder_container_image           = "${module.ecr_repository_recorder.repository_url}:${var.release_ids["recorder"]}"
  matcher_container_image            = "${module.ecr_repository_matcher.repository_url}:${var.release_ids["matcher"]}"
  merger_container_image             = "${module.ecr_repository_merger.repository_url}:${var.release_ids["merger"]}"
  id_minter_container_image          = "${module.ecr_repository_id_minter.repository_url}:${var.release_ids["id_minter"]}"
  ingestor_container_image           = "${module.ecr_repository_ingestor.repository_url}:${var.release_ids["ingestor"]}"

  sierra_merged_items_topic_name = "${data.terraform_remote_state.sierra_adapter.merged_items_topic_name}"
  sierra_merged_bibs_topic_name  = "${data.terraform_remote_state.sierra_adapter.merged_bibs_topic_name}"

  vhs_sierra_read_policy      = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sierra_read_policy}"
  vhs_sierra_bucket_name      = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sierra_bucket_name}"
  vhs_sierra_table_stream_arn = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sierra_table_stream_arn}"

  vhs_sourcedata_read_policy      = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sourcedata_read_policy}"
  vhs_sourcedata_bucket_name      = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sourcedata_bucket_name}"
  vhs_sourcedata_table_stream_arn = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sourcedata_table_stream_arn}"

  identifiers_rds_cluster_password = "${data.terraform_remote_state.catalogue_pipeline_data.identifiers_rds_cluster_password}"
  identifiers_rds_cluster_username = "${data.terraform_remote_state.catalogue_pipeline_data.identifiers_rds_cluster_username}"
  identifiers_rds_cluster_port     = "${data.terraform_remote_state.catalogue_pipeline_data.identifiers_rds_cluster_port}"
  identifiers_rds_cluster_host     = "${data.terraform_remote_state.catalogue_pipeline_data.identifiers_rds_cluster_host}"

  rds_access_security_group_id = "${data.terraform_remote_state.catalogue_pipeline_data.rds_access_security_group_id}"
}
