locals {
  ec2_terminating_topic_arn                       = "${data.terraform_remote_state.shared_infra.ec2_terminating_topic_arn}"
  ec2_instance_terminating_for_too_long_alarm_arn = "${data.terraform_remote_state.shared_infra.ec2_instance_terminating_for_too_long_alarm_arn}"
  ec2_terminating_topic_publish_policy            = "${data.terraform_remote_state.shared_infra.ec2_terminating_topic_publish_policy}"
  vpc_id                                          = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_id}"
  private_subnets                                 = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_private_subnets}"
  goobi_reader_container_image                    = "${module.ecr_repository_goobi_reader.repository_url}:${var.release_ids["goobi_reader"]}"

  vhs_goobi_full_access_policy = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_goobi_full_access_policy}"
  vhs_goobi_table_name         = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_goobi_table_name}"
  vhs_goobi_bucket_name        = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_goobi_bucket_name}"
}
