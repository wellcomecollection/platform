locals {
  vhs_sierra_table_name       = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sierra_table_name}"
  vhs_miro_table_name         = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_miro_table_name}"
  vhs_sierra_items_table_name = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sierra_items_table_name}"

  private_subnets = "${data.terraform_remote_state.shared_infra.catalogue_private_subnets}"

  dlq_alarm_arn = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"

  reporting_miro_hybrid_records_topic_arn              = "${data.terraform_remote_state.shared_infra.reporting_miro_reindex_topic_arn}"
  reporting_miro_hybrid_records_topic_publish_policy   = "${data.terraform_remote_state.shared_infra.reporting_miro_reindex_topic_publish_policy}"

  reporting_sierra_hybrid_records_topic_arn            = "${data.terraform_remote_state.shared_infra.reporting_sierra_reindex_topic_arn}"
  reporting_sierra_hybrid_records_topic_publish_policy = "${data.terraform_remote_state.shared_infra.reporting_sierra_reindex_topic_publish_policy}"

  catalogue_miro_hybrid_records_topic_arn              = "${data.terraform_remote_state.shared_infra.catalogue_miro_reindex_topic_arn}"
  catalogue_miro_hybrid_records_topic_publish_policy   = "${data.terraform_remote_state.shared_infra.catalogue_miro_reindex_topic_publish_policy}"

  catalogue_sierra_hybrid_records_topic_arn            = "${data.terraform_remote_state.shared_infra.catalogue_sierra_reindex_topic_arn}"
  catalogue_sierra_hybrid_records_topic_publish_policy = "${data.terraform_remote_state.shared_infra.catalogue_sierra_reindex_topic_publish_policy}"

  catalogue_sierra_items_hybrid_records_topic_arn            = "${data.terraform_remote_state.shared_infra.catalogue_sierra_items_reindex_topic_arn}"
  catalogue_sierra_items_hybrid_records_topic_publish_policy = "${data.terraform_remote_state.shared_infra.catalogue_sierra_items_reindex_topic_publish_policy}"

  reindex_worker_container_image = "${module.ecr_repository_reindex_worker.repository_url}:${var.release_ids["reindex_worker"]}"

  vpc_id          = "${data.terraform_remote_state.shared_infra.catalogue_vpc_id}"
}
