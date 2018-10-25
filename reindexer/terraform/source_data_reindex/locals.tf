locals {
  vhs_sierra_table_name       = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sierra_table_name}"
  vhs_miro_table_name         = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_miro_table_name}"
  vhs_sierra_items_table_name = "${data.terraform_remote_state.catalogue_pipeline_data.vhs_sierra_items_table_name}"

  vpc_id          = "${data.terraform_remote_state.shared_infra.catalogue_vpc_id}"
  private_subnets = "${data.terraform_remote_state.shared_infra.catalogue_private_subnets}"

  dlq_alarm_arn = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
}

data "aws_ecs_cluster" "cluster" {
  cluster_name = "${var.cluster_name}"
}
