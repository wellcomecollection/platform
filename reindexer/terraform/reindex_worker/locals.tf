locals {
  vpc_id          = "${data.terraform_remote_state.shared_infra.catalogue_vpc_id}"
  private_subnets = "${data.terraform_remote_state.shared_infra.catalogue_private_subnets}"
  dlq_alarm_arn   = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
}
