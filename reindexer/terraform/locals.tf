locals {
  vhs_sierra_table_name         = "${data.terraform_remote_state.infra_crtical.vhs_sierra_table_name}"
  vhs_miro_table_name           = "${data.terraform_remote_state.infra_crtical.vhs_miro_table_name}"
  vhs_miro_inventory_table_name = "${data.terraform_remote_state.infra_crtical.vhs_miro_inventory_table_name}"
  vhs_sierra_items_table_name   = "${data.terraform_remote_state.infra_crtical.vhs_sierra_items_table_name}"

  dlq_alarm_arn = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"

  reporting_miro_hybrid_records_topic_arn           = "${data.terraform_remote_state.shared_infra.reporting_miro_reindex_topic_arn}"
  reporting_miro_inventory_hybrid_records_topic_arn = "${data.terraform_remote_state.shared_infra.reporting_miro_inventory_reindex_topic_arn}"
  reporting_sierra_hybrid_records_topic_arn         = "${data.terraform_remote_state.shared_infra.reporting_sierra_reindex_topic_arn}"
  catalogue_miro_hybrid_records_topic_arn           = "${data.terraform_remote_state.shared_infra.catalogue_miro_reindex_topic_arn}"
  catalogue_sierra_hybrid_records_topic_arn         = "${data.terraform_remote_state.shared_infra.catalogue_sierra_reindex_topic_arn}"
  catalogue_sierra_items_hybrid_records_topic_arn   = "${data.terraform_remote_state.shared_infra.catalogue_sierra_items_reindex_topic_arn}"

  reindex_worker_container_image = "${module.ecr_repository_reindex_worker.repository_url}:${var.release_ids["reindex_worker"]}"

  vpc_id          = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_id}"
  private_subnets = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_private_subnets}"

  # This map defines the possible reindexer configurations.
  #
  # The key is the "ID" that can be used to trigger a reindex, and the table/topic
  # are the DynamoDB table that will be reindexed, and the topic ARN to send
  # new records to, respectively.
  #
  reindexer_jobs = [
    {
      id    = "sierra--reporting"
      table = "${local.vhs_sierra_table_name}"
      topic = "${local.reporting_sierra_hybrid_records_topic_arn}"
    },
    {
      id    = "sierra--catalogue"
      table = "${local.vhs_sierra_table_name}"
      topic = "${local.catalogue_sierra_hybrid_records_topic_arn}"
    },
    {
      id    = "miro--reporting"
      table = "${local.vhs_miro_table_name}"
      topic = "${local.reporting_miro_hybrid_records_topic_arn}"
    },
    {
      id    = "miro--catalogue"
      table = "${local.vhs_miro_table_name}"
      topic = "${local.catalogue_miro_hybrid_records_topic_arn}"
    },
    {
      id    = "miro_inventory--reporting"
      table = "${local.vhs_miro_inventory_table_name}"
      topic = "${local.reporting_miro_inventory_hybrid_records_topic_arn}"
    },
    {
      id    = "sierra_items--catalogue"
      table = "${local.vhs_sierra_items_table_name}"
      topic = "${local.catalogue_sierra_items_hybrid_records_topic_arn}"
    },
  ]
}
