locals {
  vpc_id          = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_id}"
  private_subnets = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_private_subnets}"
  dlq_alarm_arn   = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
}
