locals {
  container_image = "${module.ecr_repository_sierra_to_dynamo.repository_url}:${var.release_id}"

  vhs_sierra_items_full_access_policy = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sierra_items_full_access_policy}"
  vhs_sierra_items_table_name         = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sierra_items_table_name}"
  vhs_sierra_items_bucket_name        = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sierra_items_bucket_name}"
}
