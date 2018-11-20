locals {
  vpc_id                               = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_id}"
  public_subnets                       = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_public_subnets}"
  private_subnets                      = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_private_subnets}"

  dlq_alarm_arn   = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
}
