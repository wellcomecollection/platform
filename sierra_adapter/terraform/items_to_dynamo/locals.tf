locals {
  container_image = "${module.ecr_repository_sierra_to_dynamo.repository_url}:20e11e803698059bbec56d73b6d653a2c01c0e50"

  vhs_sierra_items_full_access_policy = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sierra_items_full_access_policy}"
  vhs_sierra_items_table_name         = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sierra_items_table_name}"
  vhs_sierra_items_bucket_name        = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sierra_items_bucket_name}"
}
